package com.burhankhanzada.opaquebehavioureditor.ui;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.burhankhanzada.opaquebehavioureditor.model.BodyEntry;

public class BodyListComposite extends Composite {

    private TableViewer entryViewer;
    private Button addButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;

    private List<BodyEntry> entries;
    private Consumer<Integer> selectionListener;
    private Runnable listModificationListener;

    public BodyListComposite(Composite parent, int style, List<BodyEntry> entries) {
        super(parent, style);
        this.entries = entries;
        
        setLayout(new GridLayout(1, false));
        
        Label lbl = new Label(this, SWT.NONE);
        lbl.setText("Body entries:");

        Composite row = new Composite(this, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        row.setLayout(new GridLayout(2, false));

        entryViewer = new TableViewer(row, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        GridData tvGD = new GridData(SWT.FILL, SWT.FILL, true, true);
        tvGD.heightHint = 90;
        entryViewer.getTable().setLayoutData(tvGD);
        entryViewer.setContentProvider(ArrayContentProvider.getInstance());
        entryViewer.setLabelProvider(new BodyEntryLabelProvider(entries));
        entryViewer.setInput(entries);
        
        entryViewer.addSelectionChangedListener(event -> {
            int idx = getSelectedIndex();
            updateButtonStates();
            if (selectionListener != null) {
                selectionListener.accept(idx);
            }
        });

        Composite btnCol = new Composite(row, SWT.NONE);
        btnCol.setLayout(new GridLayout(1, true));
        btnCol.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

        addButton    = ThemeUtils.createPushButton(btnCol, "Add");
        removeButton = ThemeUtils.createPushButton(btnCol, "Remove");
        upButton     = ThemeUtils.createPushButton(btnCol, "Up");
        downButton   = ThemeUtils.createPushButton(btnCol, "Down");

        addButton.addListener(SWT.Selection, e -> onAdd());
        removeButton.addListener(SWT.Selection, e -> onRemove());
        upButton.addListener(SWT.Selection, e -> onMove(-1));
        downButton.addListener(SWT.Selection, e -> onMove(1));
        
        updateButtonStates();
    }
    
    public void setSelectionListener(Consumer<Integer> listener) {
        this.selectionListener = listener;
    }
    
    public void setListModificationListener(Runnable listener) {
        this.listModificationListener = listener;
    }

    public int getSelectedIndex() {
        BodyEntry sel = (BodyEntry) entryViewer.getStructuredSelection().getFirstElement();
        if (sel == null) return -1;
        return entries.indexOf(sel);
    }
    
    public void selectEntry(int index) {
        if (index >= 0 && index < entries.size()) {
            entryViewer.getTable().select(index);
        } else {
            entryViewer.getTable().deselectAll();
        }
        updateButtonStates();
    }

    public void refresh() {
        if (entryViewer != null && !entryViewer.getTable().isDisposed()) {
            entryViewer.refresh();
        }
    }

    private void onAdd() {
        BodyEntry newEntry = new BodyEntry("CPP", "");
        entries.add(newEntry);
        entryViewer.refresh();
        entryViewer.setSelection(new StructuredSelection(newEntry), true);
        
        if (listModificationListener != null) listModificationListener.run();
    }

    private void onRemove() {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return;
        
        entries.remove(selectedIndex);
        entryViewer.refresh();
        
        if (entries.isEmpty()) {
            // Nothing selected
        } else {
            int newIdx = Math.min(selectedIndex, entries.size() - 1);
            entryViewer.getTable().select(newIdx);
            
            // Explicitly fire selection changed if we force-select a new one
            if (selectionListener != null) {
                selectionListener.accept(newIdx);
            }
        }
        updateButtonStates();
        
        if (listModificationListener != null) listModificationListener.run();
    }

    private void onMove(int direction) {
        int selectedIndex = getSelectedIndex();
        if (selectedIndex < 0) return;
        int target = selectedIndex + direction;
        if (target < 0 || target >= entries.size()) return;
        
        BodyEntry entry = entries.remove(selectedIndex);
        entries.add(target, entry);
        entryViewer.refresh();
        entryViewer.getTable().select(target);
        updateButtonStates();
        
        // Notify of move selection change
        if (selectionListener != null) {
            selectionListener.accept(target);
        }
        if (listModificationListener != null) listModificationListener.run();
    }

    private void updateButtonStates() {
        int selectedIndex = getSelectedIndex();
        boolean hasSel = selectedIndex >= 0;
        removeButton.setEnabled(hasSel);
        upButton.setEnabled(hasSel && selectedIndex > 0);
        downButton.setEnabled(hasSel && selectedIndex < entries.size() - 1);
    }
}
