package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.window.Window;

import com.burhankhanzada.opaquebehavioureditor.editor.text.AutoFormatter;
import com.burhankhanzada.opaquebehavioureditor.editor.ui.SimpleFindReplaceDialog;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.EditorThemeManager;

public class EditorActionManager {

    public static class KeyBindings {
        public static int UNDO = 'z';
        public static int REDO = 'y';
        public static int FORMAT = 'f';
        public static int FIND = 'f';
        public static int TOGGLE_COMMENT = '/';
        public static int ZOOM_IN_1 = '=';
        public static int ZOOM_IN_2 = '+';
        public static int ZOOM_OUT = '-';
        public static int WORD_WRAP = 'z';
        public static int DELETE_LINE = 'd';
        public static int GO_TO_LINE = 'l';
        public static int DUPLICATE_LINE = SWT.ARROW_DOWN;
        public static int RENAME = SWT.F2;
        public static int RENAME_CTRL = 'r';
    }

    private final SourceViewer sourceViewer;
    private final IUndoManager undoManager;
    private final SimpleFindReplaceDialog findDialog;
    private final EditorThemeManager themeManager;

    public EditorActionManager(SourceViewer sourceViewer, IUndoManager undoManager, 
                               SimpleFindReplaceDialog findDialog, EditorThemeManager themeManager) {
        this.sourceViewer = sourceViewer;
        this.undoManager = undoManager;
        this.findDialog = findDialog;
        this.themeManager = themeManager;
    }

    public VerifyKeyListener createVerifyKeyListener() {
        StyledText codeText = sourceViewer.getTextWidget();
        return new VerifyKeyListener() {
            @Override
            public void verifyKey(VerifyEvent e) {
                boolean isCtrl = (e.stateMask & SWT.MOD1) != 0;
                boolean isShift = (e.stateMask & SWT.SHIFT) != 0;
                boolean isAlt = (e.stateMask & SWT.ALT) != 0;
                
                if (isCtrl && e.keyCode == KeyBindings.UNDO) {
                    if (isShift) {
                        if (undoManager.redoable()) undoManager.redo();
                    } else {
                        if (undoManager.undoable()) undoManager.undo();
                    }
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.REDO) {
                    if (undoManager.redoable()) undoManager.redo();
                    e.doit = false;
                } else if (isCtrl && isShift && e.keyCode == KeyBindings.FORMAT) {
                    formatDocument();
                    e.doit = false;
                } else if (isCtrl && !isShift && e.keyCode == KeyBindings.FIND) {
                    findDialog.open();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.TOGGLE_COMMENT) {
                    toggleComments();
                    e.doit = false;
                } else if (isCtrl && (e.keyCode == KeyBindings.ZOOM_IN_1 || e.keyCode == KeyBindings.ZOOM_IN_2)) {
                    themeManager.zoomIn();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.ZOOM_OUT) {
                    themeManager.zoomOut();
                    e.doit = false;
                } else if (isAlt && e.keyCode == KeyBindings.WORD_WRAP) {
                    codeText.setWordWrap(!codeText.getWordWrap());
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.DELETE_LINE) {
                    deleteLine();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.GO_TO_LINE) {
                    goToLine();
                    e.doit = false;
                } else if (isCtrl && isAlt && e.keyCode == KeyBindings.DUPLICATE_LINE) {
                    duplicateLine();
                    e.doit = false;
                } else if (e.keyCode == KeyBindings.RENAME || (isCtrl && e.keyCode == KeyBindings.RENAME_CTRL)) {
                    renameSymbol();
                    e.doit = false;
                } else if (e.keyCode == SWT.TAB) {
                    if (isShift) {
                        sourceViewer.doOperation(ITextOperationTarget.SHIFT_LEFT);
                        e.doit = false;
                    } else if (codeText.getSelectionCount() > 0) {
                        sourceViewer.doOperation(ITextOperationTarget.SHIFT_RIGHT);
                        e.doit = false;
                    }
                }
            }
        };
    }

    private void formatDocument() {
        IDocument doc = sourceViewer.getDocument();
        StyledText codeText = sourceViewer.getTextWidget();
        String lang = (String) codeText.getData("currentLanguage");
        if (lang == null) lang = "";
        
        String currentText = doc.get();
        String formatted = AutoFormatter.format(currentText, lang);
        
        if (!currentText.equals(formatted)) {
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.set(formatted);
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
        }
    }

    private void deleteLine() {
        IDocument doc = sourceViewer.getDocument();
        Point sel = sourceViewer.getTextWidget().getSelection();
        try {
            int startLine = doc.getLineOfOffset(sel.x);
            int endLine = doc.getLineOfOffset(sel.y > sel.x ? sel.y - 1 : sel.x);
            
            int startOffset = doc.getLineOffset(startLine);
            int endOffset = doc.getLength();
            if (endLine < doc.getNumberOfLines() - 1) {
                endOffset = doc.getLineOffset(endLine + 1);
            } else if (startLine > 0) {
                startOffset = doc.getLineOffset(startLine - 1) + doc.getLineLength(startLine - 1);
                endOffset = doc.getLength();
                startOffset = doc.getLineOffset(startLine) - doc.getLineDelimiter(startLine - 1).length();
            }
            
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.replace(startOffset, endOffset - startOffset, "");
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
        } catch (Exception e) {}
    }

    private void duplicateLine() {
        IDocument doc = sourceViewer.getDocument();
        Point sel = sourceViewer.getTextWidget().getSelection();
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
            
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.replace(endOffset, 0, textToDuplicate);
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
            
            sourceViewer.getTextWidget().setSelection(endOffset, endOffset + textToDuplicate.length());
        } catch (Exception e) {}
    }

    private void goToLine() {
        Shell shell = sourceViewer.getTextWidget().getShell();
        InputDialog dialog = new InputDialog(
            shell, "Go to Line", "Enter line number (1 - " + sourceViewer.getDocument().getNumberOfLines() + "):", "",
            new IInputValidator() {
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
            
        if (dialog.open() == Window.OK) {
            try {
                int line = Integer.parseInt(dialog.getValue()) - 1;
                int offset = sourceViewer.getDocument().getLineOffset(line);
                sourceViewer.getTextWidget().setSelection(offset);
                sourceViewer.revealRange(offset, 0);
            } catch (Exception ex) {}
        }
    }

    private void toggleComments() {
        IDocument doc = sourceViewer.getDocument();
        Point sel = sourceViewer.getTextWidget().getSelection();
        try {
            int startLine = doc.getLineOfOffset(sel.x);
            int endLine = doc.getLineOfOffset(sel.y > sel.x ? sel.y - 1 : sel.x);
            
            boolean allCommented = true;
            for (int i = startLine; i <= endLine; i++) {
                IRegion lineRegion = doc.getLineInformation(i);
                String line = doc.get(lineRegion.getOffset(), lineRegion.getLength());
                if (!line.trim().isEmpty() && !line.trim().startsWith("//")) {
                    allCommented = false;
                    break;
                }
            }
            
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            
            for (int i = startLine; i <= endLine; i++) {
                IRegion lineRegion = doc.getLineInformation(i);
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
            
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
            
            int newStart = doc.getLineOffset(startLine);
            int newEnd;
            if (endLine < doc.getNumberOfLines() - 1) {
                newEnd = doc.getLineOffset(endLine + 1) - doc.getLineDelimiter(endLine).length();
            } else {
                newEnd = doc.getLength();
            }
            sourceViewer.getTextWidget().setSelection(newStart, newEnd);
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void renameSymbol() {
        IDocument doc = sourceViewer.getDocument();
        int offset = sourceViewer.getTextWidget().getCaretOffset();
        String text = doc.get();
        
        // Find word boundaries around caret
        int start = offset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        int end = offset;
        while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        
        if (start >= end) return; // Not on a word
        
        String oldName = text.substring(start, end);
        
        Shell shell = sourceViewer.getTextWidget().getShell();
        InputDialog dialog = new InputDialog(
            shell, "Rename Symbol", "Enter new name for '" + oldName + "':", oldName,
            new IInputValidator() {
                @Override
                public String isValid(String newText) {
                    if (newText.trim().isEmpty()) return "Name cannot be empty.";
                    if (newText.equals(oldName)) return "Name must be different.";
                    return null;
                }
            });
            
        if (dialog.open() == Window.OK) {
            String newName = dialog.getValue().trim();
            
            if (sourceViewer instanceof ITextViewerExtension) {
                ((ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            
            try {
                // Find and replace all whole-word occurrences of oldName
                Pattern p = Pattern.compile("\\b" + Pattern.quote(oldName) + "\\b");
                Matcher m = p.matcher(text);
                
                // Replace from back to front to avoid offset shifting issues
                List<Integer> offsets = new ArrayList<>();
                while (m.find()) {
                    offsets.add(0, m.start());
                }
                
                for (int occOffset : offsets) {
                    doc.replace(occOffset, oldName.length(), newName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (sourceViewer instanceof ITextViewerExtension) {
                    ((ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
                }
            }
        }
    }
}
