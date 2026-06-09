package com.burhankhanzada.opaquebehavioureditor.editor;

import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SimpleFindReplaceDialog {

    private final Shell parentShell;
    private final IFindReplaceTarget target;
    private Shell shell;

    public SimpleFindReplaceDialog(Shell parentShell, IFindReplaceTarget target) {
        this.parentShell = parentShell;
        this.target = target;
    }

    public void open() {
        if (shell != null && !shell.isDisposed()) {
            shell.forceActive();
            return;
        }

        shell = new Shell(parentShell, SWT.TITLE | SWT.CLOSE | SWT.MODELESS | SWT.BORDER);
        shell.setText("Find / Replace");
        shell.setLayout(new GridLayout(2, false));

        Label findLabel = new Label(shell, SWT.NONE);
        findLabel.setText("Find:");
        Text findText = new Text(shell, SWT.BORDER);
        findText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label replaceLabel = new Label(shell, SWT.NONE);
        replaceLabel.setText("Replace:");
        Text replaceText = new Text(shell, SWT.BORDER);
        replaceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button findButton = new Button(shell, SWT.PUSH);
        findButton.setText("Find Next");
        findButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        
        Button replaceButton = new Button(shell, SWT.PUSH);
        replaceButton.setText("Replace");
        replaceButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        Button replaceAllButton = new Button(shell, SWT.PUSH);
        replaceAllButton.setText("Replace All");
        replaceAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String search = findText.getText();
                if (!search.isEmpty()) {
                    target.findAndSelect(-1, search, true, false, false);
                }
            }
        });

        replaceButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String replace = replaceText.getText();
                try {
                    target.replaceSelection(replace);
                    String search = findText.getText();
                    if (!search.isEmpty()) {
                        target.findAndSelect(-1, search, true, false, false);
                    }
                } catch (Exception ex) {}
            }
        });

        replaceAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String search = findText.getText();
                String replace = replaceText.getText();
                if (!search.isEmpty()) {
                    int offset = 0;
                    while (target.findAndSelect(offset, search, true, false, false) != -1) {
                        target.replaceSelection(replace);
                        Point sel = target.getSelection();
                        offset = sel.x + sel.y; // Advance past the replaced text
                    }
                }
            }
        });

        shell.setDefaultButton(findButton);
        shell.pack();
        
        // Center on parent
        Point parentLoc = parentShell.getLocation();
        Point parentSize = parentShell.getSize();
        Point mySize = shell.getSize();
        shell.setLocation(parentLoc.x + (parentSize.x - mySize.x) / 2, parentLoc.y + (parentSize.y - mySize.y) / 2);
        
        shell.open();
    }
}
