package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.burhankhanzada.opaquebehavioureditor.editor.text.AutoFormatter;

public class FormatDocumentAction {

    private final SourceViewer sourceViewer;

    public FormatDocumentAction(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void execute() {
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
}
