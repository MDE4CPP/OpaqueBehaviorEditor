package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;

public class ToggleCommentsAction {

    private final SourceViewer sourceViewer;

    public ToggleCommentsAction(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void execute() {
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
}
