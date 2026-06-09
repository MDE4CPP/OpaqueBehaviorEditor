package com.burhankhanzada.opaquebehavioureditor.editor.ui;

import org.eclipse.jface.text.IFindReplaceTarget;
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

import com.burhankhanzada.opaquebehavioureditor.editor.core.CodeEditorConfigurator;

public class SimpleFindReplaceDialog {

    private final Shell parentShell;
    private final IFindReplaceTarget target;
    private final CodeEditorConfigurator configurator;
    private Shell shell;

    public SimpleFindReplaceDialog(Shell parentShell, IFindReplaceTarget target, CodeEditorConfigurator configurator) {
        this.parentShell = parentShell;
        this.target = target;
        this.configurator = configurator;
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
        GridData findGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        findGd.widthHint = 250;
        findText.setLayoutData(findGd);

        Label replaceLabel = new Label(shell, SWT.NONE);
        replaceLabel.setText("Replace:");
        Text replaceText = new Text(shell, SWT.BORDER);
        GridData replaceGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        replaceGd.widthHint = 250;
        replaceText.setLayoutData(replaceGd);

        Button findButton = new Button(shell, SWT.PUSH);
        findButton.setText("Find Next");
        findButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        
        Button findAllButton = new Button(shell, SWT.PUSH);
        findAllButton.setText("Find All");
        findAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        
        Button replaceButton = new Button(shell, SWT.PUSH);
        replaceButton.setText("Replace");
        replaceButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        Button replaceAllButton = new Button(shell, SWT.PUSH);
        replaceAllButton.setText("Replace All");
        replaceAllButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

        findButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                configurator.highlightSearch(null); // Clear highlight on normal find
                String search = findText.getText();
                if (!search.isEmpty()) {
                    Point sel = target.getSelection();
                    int startOffset = sel.x + sel.y; // start from end of current selection
                    if (target.findAndSelect(startOffset, search, true, false, false) == -1) {
                        // If not found, wrap around to the beginning
                        target.findAndSelect(0, search, true, false, false);
                    }
                }
            }
        });

        findAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String search = findText.getText();
                configurator.highlightSearch(search.isEmpty() ? null : search);
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
                        Point sel = target.getSelection();
                        int startOffset = sel.x + sel.y;
                        if (target.findAndSelect(startOffset, search, true, false, false) == -1) {
                            target.findAndSelect(0, search, true, false, false);
                        }
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
        
        shell.addDisposeListener(e -> configurator.highlightSearch(null));
        
        // Center on parent
        Point parentLoc = parentShell.getLocation();
        Point parentSize = parentShell.getSize();
        Point mySize = shell.getSize();
        shell.setLocation(parentLoc.x + (parentSize.x - mySize.x) / 2, parentLoc.y + (parentSize.y - mySize.y) / 2);
        
        shell.open();
    }
}
