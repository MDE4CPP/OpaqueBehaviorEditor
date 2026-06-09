package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

import com.burhankhanzada.opaquebehavioureditor.editor.text.AutoFormatter;
import com.burhankhanzada.opaquebehavioureditor.editor.ui.SimpleFindReplaceDialog;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.EditorThemeManager;

public class EditorActionManager {

    private final SourceViewer sourceViewer;
    private final org.eclipse.jface.text.IUndoManager undoManager;
    private final SimpleFindReplaceDialog findDialog;
    private final EditorThemeManager themeManager;

    public EditorActionManager(SourceViewer sourceViewer, org.eclipse.jface.text.IUndoManager undoManager, 
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
                    formatDocument();
                    e.doit = false;
                } else if (isCtrl && !isShift && e.keyCode == 'f') {
                    findDialog.open();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == '/') {
                    toggleComments();
                    e.doit = false;
                } else if (isCtrl && (e.keyCode == '=' || e.keyCode == '+')) {
                    themeManager.zoomIn();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == '-') {
                    themeManager.zoomOut();
                    e.doit = false;
                } else if (isAlt && e.keyCode == 'z') {
                    codeText.setWordWrap(!codeText.getWordWrap());
                    e.doit = false;
                } else if (isCtrl && e.keyCode == 'd') {
                    deleteLine();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == 'l') {
                    goToLine();
                    e.doit = false;
                } else if (isCtrl && isAlt && e.keyCode == SWT.ARROW_DOWN) {
                    duplicateLine();
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
            }
        };
    }

    private void formatDocument() {
        org.eclipse.jface.text.IDocument doc = sourceViewer.getDocument();
        StyledText codeText = sourceViewer.getTextWidget();
        String lang = (String) codeText.getData("currentLanguage");
        if (lang == null) lang = "";
        
        String currentText = doc.get();
        String formatted = AutoFormatter.format(currentText, lang);
        
        if (!currentText.equals(formatted)) {
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.set(formatted);
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
        }
    }

    private void deleteLine() {
        org.eclipse.jface.text.IDocument doc = sourceViewer.getDocument();
        org.eclipse.swt.graphics.Point sel = sourceViewer.getTextWidget().getSelection();
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
            
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().beginCompoundChange();
            }
            doc.replace(startOffset, endOffset - startOffset, "");
            if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension) {
                ((org.eclipse.jface.text.ITextViewerExtension) sourceViewer).getRewriteTarget().endCompoundChange();
            }
        } catch (Exception e) {}
    }

    private void duplicateLine() {
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

    private void goToLine() {
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

    private void toggleComments() {
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
