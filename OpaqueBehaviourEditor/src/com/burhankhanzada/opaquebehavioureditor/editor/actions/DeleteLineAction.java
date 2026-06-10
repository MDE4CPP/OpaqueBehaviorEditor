package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.graphics.Point;

public class DeleteLineAction {

    private final SourceViewer sourceViewer;

    public DeleteLineAction(SourceViewer sourceViewer) {
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
}
