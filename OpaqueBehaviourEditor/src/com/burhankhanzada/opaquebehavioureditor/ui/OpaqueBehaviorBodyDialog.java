package com.burhankhanzada.opaquebehavioureditor.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.burhankhanzada.opaquebehavioureditor.StringConstants;
import com.burhankhanzada.opaquebehavioureditor.model.BodyEntry;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;

/**
 * Dialog for editing the body entries of a UML OpaqueBehaviour.
 * Refactored to delegate to BodyListComposite and CodeEditorComposite.
 */
public class OpaqueBehaviorBodyDialog extends TitleAreaDialog {

    private List<BodyEntry> entries = new ArrayList<>();
    private final String behaviourName;
    
    private BodyListComposite bodyList;
    private CodeEditorComposite codeEditor;

    private final ModelDictionary dictionary;
    private final ISelectionProvider selectionProvider;
    private Runnable saveAction;

    private final boolean isUml;

    public OpaqueBehaviorBodyDialog(Shell parentShell,
                                    List<String> bodies,
                                    List<String> languages,
                                    String name,
                                    Set<String> contextTypes,
                                    ModelDictionary dictionary,
                                    ISelectionProvider selectionProvider,
                                    boolean isUml) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        this.behaviourName = name;
        this.dictionary = dictionary;
        this.selectionProvider = selectionProvider;
        this.isUml = isUml;

        for (int i = 0; i < bodies.size(); i++) {
            String lang = (i < languages.size()) ? languages.get(i) : "";
            entries.add(new BodyEntry(lang, bodies.get(i)));
        }
    }

    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction;
    }

    public List<String> getBodies() {
        List<String> result = new ArrayList<>(entries.size());
        for (BodyEntry e : entries) result.add(e.body);
        return result;
    }

    public List<String> getLanguages() {
        List<String> result = new ArrayList<>(entries.size());
        for (BodyEntry e : entries) result.add(e.language);
        return result;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(StringConstants.DIALOG_TITLE);
    }

    @Override
    protected Point getInitialSize() {
        return new Point(1100, 800);
    }

    @Override
    protected Point getInitialLocation(Point initialSize) {
        org.eclipse.swt.graphics.Rectangle displayBounds;
        if (getParentShell() != null && getParentShell().getMonitor() != null) {
            displayBounds = getParentShell().getMonitor().getBounds();
        } else {
            displayBounds = Display.getCurrent().getPrimaryMonitor().getBounds();
        }
        int x = displayBounds.x + (displayBounds.width - initialSize.x) / 2;
        int y = displayBounds.y + (displayBounds.height - initialSize.y) / 2;
        return new Point(Math.max(displayBounds.x, x), Math.max(displayBounds.y, y));
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        setTitle(StringConstants.DIALOG_TITLE);
        setMessage(behaviourName != null && !behaviourName.isBlank()
                ? StringConstants.DIALOG_MSG_EDITING + behaviourName
                : StringConstants.DIALOG_MSG_DEFAULT);
        
        Composite main = new Composite(area, SWT.NONE);
        main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        main.setLayout(new GridLayout(1, false));

        bodyList = new BodyListComposite(main, SWT.NONE, entries);
        GridData topGD = new GridData(SWT.FILL, SWT.FILL, true, false);
        if (!isUml) {
            topGD.exclude = true;
            bodyList.setVisible(false);
        }
        bodyList.setLayoutData(topGD);

        codeEditor = new CodeEditorComposite(main, SWT.NONE, dictionary, selectionProvider, isUml);
        codeEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        codeEditor.setSaveAction(saveAction);

        bodyList.setSelectionListener(index -> {
            if (index >= 0 && index < entries.size()) {
                codeEditor.loadEntry(entries.get(index));
            } else {
                codeEditor.loadEntry(null);
            }
        });



        codeEditor.setChangeListener((lang, body) -> {
            int sel = bodyList.getSelectedIndex();
            if (sel >= 0 && sel < entries.size()) {
                BodyEntry entry = entries.get(sel);
                entry.language = lang;
                entry.body = body;
                bodyList.refresh();
            }
        });

        if (!entries.isEmpty()) {
            bodyList.selectEntry(0);
            codeEditor.loadEntry(entries.get(0));
        } else {
            codeEditor.loadEntry(null);
        }

        return area;
    }


    @Override
    public boolean close() {
        if (codeEditor != null) {
            codeEditor.dispose();
        }
        if (bodyList != null) {
            bodyList.dispose();
        }
        return super.close();
    }
}
