package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

public class RenameSymbolAction {

    private final SourceViewer sourceViewer;

    public RenameSymbolAction(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void execute() {
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
