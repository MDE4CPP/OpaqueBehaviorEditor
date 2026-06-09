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
import com.burhankhanzada.opaquebehavioureditor.model.UmlModelValidator;
import com.burhankhanzada.opaquebehavioureditor.ui.ThemeUtils;

public class CodeEditorConfigurator {

    private final SemanticHighlighter semanticHighlighter;
    private final UmlModelValidator modelValidator;
    
    private TMPresentationReconciler tmReconciler;
    private Font monoFont;
    private Color umlTypeColor;
    private Color methodColor;
    private Color variableColor;

    public CodeEditorConfigurator(SemanticHighlighter semanticHighlighter, UmlModelValidator modelValidator) {
        this.semanticHighlighter = semanticHighlighter;
        this.modelValidator = modelValidator;
    }

    public void configure(Composite parent, SourceViewer sourceViewer) {
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
    }

    private void setupEditorFontAndColors(Composite parent, StyledText codeText) {
        Display display = parent.getDisplay();
        // Monospace font
        monoFont = new Font(display, new FontData("Menlo", 12, SWT.NORMAL));
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
            separatorColor = new Color(display, new RGB(64, 64, 64));
            
            umlTypeColor = new Color(display, 78, 201, 176);
            methodColor = new Color(display, 220, 220, 170);
            variableColor = new Color(display, 156, 220, 254);
        } else {
            lineNumColor = new Color(display, new RGB(43, 145, 175));
            separatorColor = new Color(display, new RGB(200, 200, 200));
            
            // VS Code light theme semantic colors
            umlTypeColor = new Color(display, 38, 127, 153); // Dark teal (#267f99)
            methodColor = new Color(display, 121, 94, 38);   // Dark yellow/brown (#795e26)
            variableColor = new Color(display, 0, 16, 128);  // Dark blue (#001080)
        }

        // Clean up colors
        codeText.addDisposeListener(e -> {
            if (lineNumColor != null) lineNumColor.dispose();
            if (separatorColor != null) separatorColor.dispose();
            if (umlTypeColor != null) umlTypeColor.dispose();
            if (methodColor != null) methodColor.dispose();
            if (variableColor != null) variableColor.dispose();
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
                    
                    // Variable Highlighting
                    List<TextRange> varRanges = semanticHighlighter.getVariableRanges(codeText.getText(), langDef);
                    for (TextRange vr : varRanges) {
                        StyleRange style = new StyleRange(vr.offset, vr.length, variableColor, null);
                        textPresentation.mergeStyleRange(style);
                    }
                    
                    // Errors
                    List<TextRange> errors = modelValidator.validateUMLMemberAccess(codeText.getText(), langDef);
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
    }
}
