package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

public class GoToLineAction {

    private final SourceViewer sourceViewer;

    public GoToLineAction(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void execute() {
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
}
