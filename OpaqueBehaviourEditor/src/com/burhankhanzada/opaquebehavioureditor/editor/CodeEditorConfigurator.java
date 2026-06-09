package com.burhankhanzada.opaquebehavioureditor.editor;

import java.util.List;

import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.presentation.IPresentationReconciler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;

import com.burhankhanzada.opaquebehavioureditor.model.TextRange;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;
import com.burhankhanzada.opaquebehavioureditor.ui.ThemeUtils;

public class CodeEditorConfigurator {

    private final SemanticHighlighter semanticHighlighter;
    private final ModelValidator modelValidator;
    
    private TMPresentationReconciler tmReconciler;
    private Font monoFont;
    private Color umlTypeColor;
    private Color methodColor;
    private Color variableColor;
    private Color keywordColor;
    private Color commentColor;
    private Color stringColor;
    
    private Color searchHighlightColor;
    private String currentSearchText;
    
    private int currentFontSize = 13;
    private SourceViewer sourceViewer;

    public CodeEditorConfigurator(SemanticHighlighter semanticHighlighter, ModelValidator modelValidator) {
        this.semanticHighlighter = semanticHighlighter;
        this.modelValidator = modelValidator;
    }

    public void configure(Composite parent, SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
        StyledText codeText = sourceViewer.getTextWidget();

        // Prevent ESC from closing the dialog while in the code editor
        codeText.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_ESCAPE) {
                e.doit = false;
            }
        });

        setupEditorFontAndColors(parent, codeText);
        setupLineNumbers(codeText);
        setupSyntaxHighlighting(sourceViewer, codeText);
        setupBracketMatching(sourceViewer);
        setupCurrentLineHighlighting(sourceViewer);

        // ---- Attach Undo/Redo Manager ----
        org.eclipse.jface.text.IUndoManager undoManager = new org.eclipse.jface.text.TextViewerUndoManager(200);
        undoManager.connect(sourceViewer);
        
        SimpleFindReplaceDialog findDialog = new SimpleFindReplaceDialog(parent.getShell(), sourceViewer.getFindReplaceTarget(), this);
        
        codeText.addVerifyKeyListener(e -> {
            boolean isCtrl = (e.stateMask & SWT.MOD1) != 0;
            boolean isShift = (e.stateMask & SWT.SHIFT) != 0;
            boolean isAlt = (e.stateMask & SWT.ALT) != 0;
            
            if (isCtrl && e.keyCode == 'z') {
                if (isShift) {
                    if (undoManager.redoable()) undoManager.redo();
                } else {
                    if (undoManager.undoable()) undoManager.undo();
                }
                e.doit = false;
            } else if (isCtrl && e.keyCode == 'y') {
                if (undoManager.redoable()) undoManager.redo();
                e.doit = false;
            } else if (isCtrl && isShift && e.keyCode == 'f') {
                formatDocument(sourceViewer);
                e.doit = false;
            } else if (isCtrl && !isShift && e.keyCode == 'f') {
                findDialog.open();
                e.doit = false;
            } else if (isCtrl && e.keyCode == '/') {
                toggleComments(sourceViewer);
                e.doit = false;
            } else if (isCtrl && (e.keyCode == '=' || e.keyCode == '+')) {
                zoomIn();
                e.doit = false;
            } else if (isCtrl && e.keyCode == '-') {
                zoomOut();
                e.doit = false;
            } else if (isAlt && e.keyCode == 'z') {
                codeText.setWordWrap(!codeText.getWordWrap());
                e.doit = false;
            } else if (isCtrl && e.keyCode == 'd') {
                deleteLine(sourceViewer);
                e.doit = false;
            } else if (isCtrl && e.keyCode == 'l') {
                goToLine(sourceViewer);
                e.doit = false;
            } else if (isCtrl && isAlt && e.keyCode == SWT.ARROW_DOWN) {
                duplicateLine(sourceViewer);
                e.doit = false;
            } else if (e.keyCode == SWT.TAB) {
                if (isShift) {
                    sourceViewer.doOperation(org.eclipse.jface.text.ITextOperationTarget.SHIFT_LEFT);
                    e.doit = false;
                } else if (codeText.getSelectionCount() > 0) {
                    sourceViewer.doOperation(org.eclipse.jface.text.ITextOperationTarget.SHIFT_RIGHT);
                    e.doit = false;
                }
            }
        });
    }

    private void formatDocument(SourceViewer sourceViewer) {
        org.eclipse.jface.text.IDocument doc = sourceViewer.getDocument();
        StyledText codeText = sourceViewer.getTextWidget();
        String lang = (String) codeText.getData("currentLanguage");
        if (lang == null) lang = "";
        
        String currentText = doc.get();
        String formatted = AutoFormatter.format(currentText, lang);
        
        if (!currentText.equals(formatted)) {
            // Use compound change so format is a single undo step
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.set(formatted);
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
        }
    }

    private void deleteLine(SourceViewer sourceViewer) {
        org.eclipse.jface.text.IDocument doc = sourceViewer.getDocument();
        org.eclipse.swt.graphics.Point sel = sourceViewer.getTextWidget().getSelection();
        try {
            int startLine = doc.getLineOfOffset(sel.x);
            int endLine = doc.getLineOfOffset(sel.y > sel.x ? sel.y - 1 : sel.x);
            
            int startOffset = doc.getLineOffset(startLine);
            int endOffset = doc.getLength(); // Default to end of document
            if (endLine < doc.getNumberOfLines() - 1) {
                endOffset = doc.getLineOffset(endLine + 1); // Up to the start of the next line
            } else if (startLine > 0) {
                // Deleting the last line, try to remove the newline before it instead
                startOffset = doc.getLineOffset(startLine - 1) + doc.getLineLength(startLine - 1);
                // Adjust to exclude the \r\n or \n from the previous line? No, just replace from start to end
                endOffset = doc.getLength();
                startOffset = doc.getLineOffset(startLine) - doc.getLineDelimiter(startLine - 1).length();
            }
            
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.replace(startOffset, endOffset - startOffset, "");
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
        } catch (Exception e) {}
    }

    private void duplicateLine(SourceViewer sourceViewer) {
        org.eclipse.jface.text.IDocument doc = sourceViewer.getDocument();
        org.eclipse.swt.graphics.Point sel = sourceViewer.getTextWidget().getSelection();
        try {
            int startLine = doc.getLineOfOffset(sel.x);
            int endLine = doc.getLineOfOffset(sel.y > sel.x ? sel.y - 1 : sel.x);
            
            int startOffset = doc.getLineOffset(startLine);
            int endOffset = doc.getLength();
            if (endLine < doc.getNumberOfLines() - 1) {
                endOffset = doc.getLineOffset(endLine + 1);
            }
            
            String textToDuplicate = doc.get(startOffset, endOffset - startOffset);
            
            if (endLine == doc.getNumberOfLines() - 1) {
                String delim = doc.getLegalLineDelimiters()[0];
                textToDuplicate = delim + textToDuplicate;
            }
            
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.replace(endOffset, 0, textToDuplicate);
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
            
            sourceViewer.getTextWidget().setSelection(endOffset, endOffset + textToDuplicate.length());
        } catch (Exception e) {}
    }

    private void goToLine(SourceViewer sourceViewer) {
        org.eclipse.swt.widgets.Shell shell = sourceViewer.getTextWidget().getShell();
        org.eclipse.jface.dialogs.InputDialog dialog = new org.eclipse.jface.dialogs.InputDialog(
            shell, "Go to Line", "Enter line number (1 - " + sourceViewer.getDocument().getNumberOfLines() + "):", "",
            new org.eclipse.jface.dialogs.IInputValidator() {
                @Override
                public String isValid(String newText) {
                    try {
                        int line = Integer.parseInt(newText);
                        if (line < 1 || line > sourceViewer.getDocument().getNumberOfLines()) {
                            return "Line number out of range.";
                        }
                        return null;
                    } catch (NumberFormatException e) {
                        return "Please enter a valid number.";
                    }
                }
            });
            
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            try {
                int line = Integer.parseInt(dialog.getValue()) - 1;
                int offset = sourceViewer.getDocument().getLineOffset(line);
                sourceViewer.getTextWidget().setSelection(offset);
                sourceViewer.revealRange(offset, 0);
            } catch (Exception ex) {}
        }
    }

    private void zoomIn() {
        if (currentFontSize < 40) {
            currentFontSize++;
            updateFont();
        }
    }

    private void zoomOut() {
        if (currentFontSize > 6) {
            currentFontSize--;
            updateFont();
        }
    }

    private void updateFont() {
        if (sourceViewer == null || sourceViewer.getTextWidget() == null || sourceViewer.getTextWidget().isDisposed()) return;
        StyledText codeText = sourceViewer.getTextWidget();
        Display display = codeText.getDisplay();
        
        Font oldFont = monoFont;
        String fontName = System.getProperty("os.name").toLowerCase().contains("mac") ? "Monaco" : "Consolas";
        monoFont = new Font(display, fontName, currentFontSize, SWT.NORMAL);
        codeText.setFont(monoFont);
        
        if (oldFont != null && !oldFont.isDisposed()) {
            oldFont.dispose();
        }
    }

    private void setupEditorFontAndColors(Composite parent, StyledText codeText) {
        Display display = parent.getDisplay();
        // Monospace font
        String fontName = System.getProperty("os.name").toLowerCase().contains("mac") ? "Monaco" : "Consolas";
        monoFont = new Font(display, fontName, currentFontSize, SWT.NORMAL);
        codeText.setFont(monoFont);
        codeText.setTabs(4);
        
        // Add left margin to reserve space for line numbers
        codeText.setMargins(45, 5, 5, 5);

        // Theme-specific colors
        boolean dark = ThemeUtils.isDarkTheme(parent);
        final Color lineNumColor;
        final Color separatorColor;
        
        if (dark) {
            Color darkBg = new Color(display, new RGB(30, 30, 30));
            Color darkFg = new Color(display, new RGB(212, 212, 212));
            Color darkSelBg = new Color(display, new RGB(38, 79, 120));
            Color darkSelFg = new Color(display, new RGB(255, 255, 255));
            
            // TODO: Fix this issue upstream in the Eclipse IDE itself.
            // WORKAROUND: The Eclipse E4 CSS engine runs a delayed layout pass on macOS that 
            // aggressively overwrites StyledText backgrounds back to the OS-default grey.
            // Trick 1: We assign a dummy CSS ID so the engine's generic StyledText rules ignore this widget.
            codeText.setData("org.eclipse.e4.ui.css.id", "UMLOpaqueBehaviourEditorText");
            
            codeText.setBackground(darkBg);
            codeText.setForeground(darkFg);
            codeText.setSelectionBackground(darkSelBg);
            codeText.setSelectionForeground(darkSelFg);
            
            // Trick 2: We forcefully re-apply the background asynchronously (after the event loop processes 
            // the initial Shell layout) to guarantee our dark background survives any late CSS styling.
            display.asyncExec(() -> {
                if (codeText != null && !codeText.isDisposed()) {
                    codeText.setBackground(darkBg);
                    codeText.setForeground(darkFg);
                    codeText.setSelectionBackground(darkSelBg);
                    codeText.setSelectionForeground(darkSelFg);
                }
            });
            
            lineNumColor = new Color(display, new RGB(133, 133, 133));
            separatorColor = new Color(display, new RGB(80, 80, 80));
            searchHighlightColor = new Color(display, new RGB(100, 100, 0)); // Dark yellow
            
            umlTypeColor = new Color(display, 78, 201, 176);
            methodColor = new Color(display, 220, 220, 170);
            variableColor = new Color(display, 156, 220, 254);
            keywordColor = new Color(display, 197, 134, 192); // Purple (#c586c0)
            commentColor = new Color(display, 96, 139, 78);   // Green (#608b4e)
            stringColor = new Color(display, 206, 145, 120);  // Orange (#ce9178)
        } else {
            lineNumColor = new Color(display, new RGB(43, 145, 175));
            separatorColor = new Color(display, new RGB(200, 200, 200));
            searchHighlightColor = new Color(display, new RGB(255, 255, 0)); // Yellow
            
            // VS Code light theme semantic colors
            umlTypeColor = new Color(display, 38, 127, 153); // Dark teal (#267f99)
            methodColor = new Color(display, 121, 94, 38);   // Dark yellow/brown (#795e26)
            variableColor = new Color(display, 0, 16, 128);  // Dark blue (#001080)
            keywordColor = new Color(display, 175, 0, 219);  // Purple (#af00db)
            commentColor = new Color(display, 0, 128, 0);    // Green (#008000)
            stringColor = new Color(display, 163, 21, 21);   // Dark Red (#a31515)
        }

        codeText.addDisposeListener(e -> {
            if (lineNumColor != null) lineNumColor.dispose();
            if (separatorColor != null) separatorColor.dispose();
            if (umlTypeColor != null) umlTypeColor.dispose();
            if (methodColor != null) methodColor.dispose();
            if (variableColor != null) variableColor.dispose();
            if (keywordColor != null) keywordColor.dispose();
            if (commentColor != null) commentColor.dispose();
            if (stringColor != null) stringColor.dispose();
            if (searchHighlightColor != null) searchHighlightColor.dispose();
        });
        
        // Save these for line number painter
        codeText.setData("lineNumColor", lineNumColor);
        codeText.setData("separatorColor", separatorColor);
    }

    private void setupLineNumbers(StyledText codeText) {
        Color lineNumColor = (Color) codeText.getData("lineNumColor");
        Color separatorColor = (Color) codeText.getData("separatorColor");
        
        // Draw line numbers inside the reserved left margin
        codeText.addPaintListener(new LineNumberPainter(codeText, lineNumColor, separatorColor));
    }

    private void setupSyntaxHighlighting(SourceViewer sourceViewer, StyledText codeText) {
        // Initialize TM4E Reconciler for generic syntax highlighting (keywords, strings, etc.)
        try {
            tmReconciler = new TMPresentationReconciler();
            sourceViewer.configure(new SourceViewerConfiguration() {
                @Override
                public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
                    return tmReconciler;
                }
                
                @Override
                public org.eclipse.jface.text.IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
                    return new org.eclipse.jface.text.IAutoEditStrategy[] { new SmartAutoEditStrategy() };
                }
                
                @Override
                public org.eclipse.jface.text.ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
                    return new SemanticTextHover(modelValidator.getDictionary());
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension4 ext4) {
            // Apply semantic highlighting (custom colors for UML types, methods, and variables)
            // AFTER the TM4E grammar has been applied, overriding it where necessary.
            ext4.addTextPresentationListener(new ITextPresentationListener() {
                @Override
                public void applyTextPresentation(TextPresentation textPresentation) {
                    String language = (String) codeText.getData("currentLanguage");
                    if (language == null) language = "";
                    LanguageMapping.LanguageDef langDef = LanguageMapping.getLanguageDef(language);
                    
                    // Type Highlighting
                    List<TextRange> typeRanges = semanticHighlighter.getUMLTypeRanges(codeText.getText(), langDef);
                    for (TextRange tr : typeRanges) {
                        StyleRange style = new StyleRange(tr.offset, tr.length, umlTypeColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // Method Highlighting
                    List<TextRange> methodRanges = semanticHighlighter.getMethodRanges(codeText.getText(), langDef);
                    for (TextRange mr : methodRanges) {
                        StyleRange style = new StyleRange(mr.offset, mr.length, methodColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // Keyword Highlighting
                    List<TextRange> keywordRanges = semanticHighlighter.getKeywordRanges(codeText.getText(), langDef);
                    for (TextRange kr : keywordRanges) {
                        StyleRange style = new StyleRange(kr.offset, kr.length, keywordColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // Variable Highlighting
                    List<TextRange> varRanges = semanticHighlighter.getVariableRanges(codeText.getText(), langDef);
                    for (TextRange vr : varRanges) {
                        StyleRange style = new StyleRange(vr.offset, vr.length, variableColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // String Highlighting
                    List<TextRange> strRanges = semanticHighlighter.getStringRanges(codeText.getText());
                    for (TextRange sr : strRanges) {
                        StyleRange style = new StyleRange(sr.offset, sr.length, stringColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // Comment Highlighting (Overrides everything else inside the comment)
                    List<TextRange> commentRanges = semanticHighlighter.getCommentRanges(codeText.getText());
                    for (TextRange cr : commentRanges) {
                        StyleRange style = new StyleRange(cr.offset, cr.length, commentColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // Search Highlighting
                    if (currentSearchText != null && !currentSearchText.isEmpty()) {
                        String fullText = codeText.getText();
                        String lowerSearch = currentSearchText.toLowerCase();
                        String lowerFull = fullText.toLowerCase();
                        int idx = lowerFull.indexOf(lowerSearch);
                        while (idx != -1) {
                            StyleRange sr = new StyleRange(idx, currentSearchText.length(), null, searchHighlightColor);
                            textPresentation.mergeStyleRange(sr);
                            idx = lowerFull.indexOf(lowerSearch, idx + currentSearchText.length());
                        }
                    }
                    
                    // Errors
                    List<TextRange> errors = modelValidator.validateMemberAccess(codeText.getText(), langDef);
                    errors.addAll(modelValidator.validateSyntax(codeText.getText(), langDef));
                    
                    for (TextRange err : errors) {
                        StyleRange style = new StyleRange(err.offset, err.length, null, null);
                        style.underline = true;
                        style.underlineStyle = SWT.UNDERLINE_ERROR;
                        style.underlineColor = codeText.getDisplay().getSystemColor(SWT.COLOR_RED);
                        textPresentation.mergeStyleRange(style);
                    }
                }
            });
        }
    }

    public void updateSyntaxLanguage(String lang, StyledText codeText) {
        if (codeText != null && !codeText.isDisposed()) {
            codeText.setData("currentLanguage", lang);
        }
        
        if (tmReconciler == null) return;
        try {
            String lower = lang == null ? "" : lang.toLowerCase();
            String ext = "txt";
            if (lower.startsWith("c++") || lower.equals("cpp")) ext = "cpp";
            else if (lower.equals("java")) ext = "java";
            else if (lower.equals("python") || lower.equals("py")) ext = "py";
            else if (lower.equals("c") || lower.matches("c[0-9]+")) ext = "c";
            
            IGrammar grammar = TMEclipseRegistryPlugin.getGrammarRegistryManager().getGrammarForFileExtension(ext);
            tmReconciler.setGrammar(grammar);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void dispose() {
        if (monoFont != null && !monoFont.isDisposed()) {
            monoFont.dispose();
        }
        if (tmReconciler != null) {
            tmReconciler.uninstall();
        }
        if (searchHighlightColor != null && !searchHighlightColor.isDisposed()) {
            searchHighlightColor.dispose();
        }
    }
    
    public void highlightSearch(String text) {
        this.currentSearchText = text;
        if (sourceViewer != null && sourceViewer.getTextWidget() != null && !sourceViewer.getTextWidget().isDisposed()) {
            sourceViewer.invalidateTextPresentation();
        }
    }

    private void setupBracketMatching(SourceViewer sourceViewer) {
        org.eclipse.jface.text.source.DefaultCharacterPairMatcher matcher = 
            new org.eclipse.jface.text.source.DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
        
        org.eclipse.jface.text.source.MatchingCharacterPainter painter = 
            new org.eclipse.jface.text.source.MatchingCharacterPainter(sourceViewer, matcher);
        
        Color matchColor = new Color(sourceViewer.getTextWidget().getDisplay(), new RGB(160, 160, 160));
        painter.setColor(matchColor);
        painter.setHighlightCharacterAtCaretLocation(true);
        
        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension2 ext2) {
            ext2.addPainter(painter);
        }
        
        sourceViewer.getTextWidget().addDisposeListener(e -> {
            if (matchColor != null) matchColor.dispose();
            if (matcher != null) matcher.dispose();
            if (painter != null) painter.deactivate(true);
        });
    }

    private void setupCurrentLineHighlighting(SourceViewer sourceViewer) {
        org.eclipse.jface.text.CursorLinePainter cursorPainter = 
            new org.eclipse.jface.text.CursorLinePainter(sourceViewer);
            
        // Subtle background color for current line (VS Code Dark style: #232323)
        Color cursorLineColor = new Color(sourceViewer.getTextWidget().getDisplay(), new RGB(40, 40, 40));
        cursorPainter.setHighlightColor(cursorLineColor);
        
        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension2 ext2) {
            ext2.addPainter(cursorPainter);
        }
        
        sourceViewer.getTextWidget().addDisposeListener(e -> {
            if (cursorLineColor != null) cursorLineColor.dispose();
            if (cursorPainter != null) cursorPainter.deactivate(true);
        });
    }

    private void toggleComments(SourceViewer sourceViewer) {
        org.eclipse.jface.text.IDocument doc = sourceViewer.getDocument();
        org.eclipse.swt.graphics.Point sel = sourceViewer.getTextWidget().getSelection();
        try {
            int startLine = doc.getLineOfOffset(sel.x);
            int endLine = doc.getLineOfOffset(sel.y > sel.x ? sel.y - 1 : sel.x);
            
            boolean allCommented = true;
            for (int i = startLine; i <= endLine; i++) {
                org.eclipse.jface.text.IRegion lineRegion = doc.getLineInformation(i);
                String line = doc.get(lineRegion.getOffset(), lineRegion.getLength());
                if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                    allCommented = false;
                    break;
                }
            }
            
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            
            for (int i = startLine; i <= endLine; i++) {
                org.eclipse.jface.text.IRegion lineRegion = doc.getLineInformation(i);
                String line = doc.get(lineRegion.getOffset(), lineRegion.getLength());
                if (allCommented) {
                    int idx = line.indexOf("//");
                    if (idx != -1) {
                        doc.replace(lineRegion.getOffset() + idx, 2, "");
                    }
                } else {
                    doc.replace(lineRegion.getOffset(), 0, "//");
                }
            }
            
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
            
            int newStart = doc.getLineOffset(startLine);
            int newEnd;
            if (endLine < doc.getNumberOfLines() - 1) {
                newEnd = doc.getLineOffset(endLine + 1) - doc.getLineDelimiter(endLine).length();
            } else {
                newEnd = doc.getLength();
            }
            sourceViewer.getTextWidget().setSelection(newStart, newEnd);
            
        } catch (org.eclipse.jface.text.BadLocationException e) {
            e.printStackTrace();
        }
    }
}
