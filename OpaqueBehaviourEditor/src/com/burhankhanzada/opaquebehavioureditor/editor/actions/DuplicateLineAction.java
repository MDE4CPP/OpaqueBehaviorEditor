package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;

public class DuplicateLineAction {

    private final SourceViewer sourceViewer;

    public DuplicateLineAction(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void execute() {
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
}
