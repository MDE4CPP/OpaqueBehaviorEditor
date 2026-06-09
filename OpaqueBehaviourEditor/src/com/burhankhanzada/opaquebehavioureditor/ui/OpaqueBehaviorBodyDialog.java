package com.burhankhanzada.opaquebehavioureditor.ui;

import com.burhankhanzada.opaquebehavioureditor.ui.*;
import com.burhankhanzada.opaquebehavioureditor.editor.*;
import com.burhankhanzada.opaquebehavioureditor.model.*;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelectionProvider;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;
import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog for editing the body entries of a UML OpaqueBehaviour.
 * Uses {@link StyledText} with {@link SyntaxHighlighter} for
 * keyword-based syntax highlighting — no JFace Text dependency.
 */
public class OpaqueBehaviorBodyDialog extends TitleAreaDialog {

    private List<BodyEntry> entries = new ArrayList<>();
    private final String behaviourName;
    private int selectedIndex = -1;

    private TableViewer entryViewer;
    private SourceViewer sourceViewer;
    private StyledText codeText;
    private Combo languageCombo;
    private Button addButton;
    private Button removeButton;
    private Button upButton;
    private Button downButton;
    private CodeCompletionProvider completionProvider;
    private final Set<String> contextTypes;
    private final UmlModelDictionary dictionary;
    private final ISelectionProvider selectionProvider;
    
    private final SemanticHighlighter semanticHighlighter;
    private final UmlModelValidator modelValidator;
    private final CodeEditorConfigurator editorConfigurator;

    private boolean suppressListener = false;

    public OpaqueBehaviorBodyDialog(Shell parentShell,
                                    List<String> bodies,
                                    List<String> languages,
                                    String name,
                                    Set<String> contextTypes,
                                    UmlModelDictionary dictionary,
                                    ISelectionProvider selectionProvider) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
        this.behaviourName = name;
        this.contextTypes = contextTypes;
        this.dictionary = dictionary;
        this.selectionProvider = selectionProvider;
        this.semanticHighlighter = new SemanticHighlighter(dictionary);
        this.modelValidator = new UmlModelValidator(dictionary);
        this.editorConfigurator = new CodeEditorConfigurator(this.semanticHighlighter, this.modelValidator);

        for (int i = 0; i < bodies.size(); i++) {
            String lang = (i < languages.size()) ? languages.get(i) : "";
            entries.add(new BodyEntry(lang, bodies.get(i)));
        }
    }

    // ---- Results ----

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

    // ---- Dialog lifecycle ----

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Edit OpaqueBehaviour Body");
    }

    @Override
    protected Point getInitialSize() {
        return new Point(850, 700);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        setTitle("Edit OpaqueBehaviour Body");
        setMessage(behaviourName != null && !behaviourName.isBlank()
                ? "Editing body of: " + behaviourName
                : "Edit body entries of the selected OpaqueBehaviour");

        Composite main = new Composite(area, SWT.NONE);
        main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        main.setLayout(new GridLayout(1, false));

        createEntrySection(main);
        createLanguageSection(main);
        createCodeSection(main);

        if (!entries.isEmpty()) {
            entryViewer.getTable().select(0);
            loadEntry(0);
        }
        updateButtonStates();

        return area;
    }

    @Override
    protected void okPressed() {
        commitCurrentEditor();
        super.okPressed();
    }

    @Override
    public boolean close() {
        if (editorConfigurator != null) {
            editorConfigurator.dispose();
        }
        if (completionProvider != null) {
            completionProvider.dispose();
        }
        return super.close();
    }

    // ---- UI construction ----

    private void createEntrySection(Composite parent) {
        Label lbl = new Label(parent, SWT.NONE);
        lbl.setText("Body entries:");

        Composite row = new Composite(parent, SWT.NONE);
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
            BodyEntry sel = (BodyEntry) event.getStructuredSelection().getFirstElement();
            if (sel != null) {
                int idx = entries.indexOf(sel);
                if (idx != selectedIndex) {
                    commitCurrentEditor();
                    loadEntry(idx);
                }
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
    }

    private void createLanguageSection(Composite parent) {
        Composite row = new Composite(parent, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        row.setLayout(new GridLayout(6, false));

        Label lbl = new Label(row, SWT.NONE);
        lbl.setText("Language:");

        languageCombo = new Combo(row, SWT.DROP_DOWN | SWT.READ_ONLY);
        languageCombo.setItems(LanguageMapping.getAllLanguageNames());
        languageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (ThemeUtils.isDarkTheme(parent)) ThemeUtils.fixComboDarkTheme(languageCombo);
        
        languageCombo.addModifyListener(e -> {
            if (selectedIndex >= 0 && selectedIndex < entries.size() && !suppressListener) {
                entries.get(selectedIndex).language = languageCombo.getText();
                editorConfigurator.updateSyntaxLanguage(languageCombo.getText(), codeText);
                if (completionProvider != null) completionProvider.setLanguage(languageCombo.getText());
                entryViewer.refresh();
            }
        });

        Button formatBtn = new Button(row, SWT.PUSH);
        formatBtn.setText("Format Code");
        formatBtn.addListener(SWT.Selection, e -> {
            if (codeText != null && !codeText.isDisposed()) {
                String formatted = AutoFormatter.format(codeText.getText(), languageCombo.getText());
                codeText.setText(formatted);
            }
        });

        Label transLbl = new Label(row, SWT.NONE);
        transLbl.setText("  Translate to:");

        Combo targetLanguageCombo = new Combo(row, SWT.DROP_DOWN | SWT.READ_ONLY);
        targetLanguageCombo.setItems(LanguageMapping.getAllLanguageNames());
        if (targetLanguageCombo.getItemCount() > 0) targetLanguageCombo.select(0);
        if (ThemeUtils.isDarkTheme(parent)) ThemeUtils.fixComboDarkTheme(targetLanguageCombo);

        Button translateBtn = new Button(row, SWT.PUSH);
        translateBtn.setText("Translate");
        translateBtn.addListener(SWT.Selection, e -> {
            if (codeText != null && !codeText.isDisposed()) {
                String sourceLang = languageCombo.getText();
                String targetLang = targetLanguageCombo.getText();
                String translated = CodeTranslator.translate(codeText.getText(), sourceLang, targetLang);
                
                // Auto-format the translated code using the target language's rules
                String formatted = AutoFormatter.format(translated, targetLang);
                
                codeText.setText(formatted);
                languageCombo.setText(targetLang); // Update main language combo
            }
        });
    }

    /**
     * Creates the code editor section containing a StyledText widget wrapped in a SourceViewer.
     * This avoids heavy JFace Text dependencies while still allowing TM4E integration.
     */
    private void createCodeSection(Composite parent) {
        Label lbl = new Label(parent, SWT.NONE);
        lbl.setText("Body code:");

        sourceViewer = new SourceViewer(parent, null, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        sourceViewer.setDocument(new Document(""));
        codeText = sourceViewer.getTextWidget();

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 350;
        gd.widthHint  = 600;
        codeText.setLayoutData(gd);

        editorConfigurator.configure(parent, sourceViewer);

        // Code Completion
        completionProvider = new CodeCompletionProvider(codeText, "", dictionary);
        completionProvider.setHyperlinkElements(selectionProvider);

        // Re-highlight on every text change
        codeText.addModifyListener(e -> {
            if (!suppressListener) {
                if (selectedIndex >= 0 && selectedIndex < entries.size()) {
                    entries.get(selectedIndex).body = codeText.getText();
                    entryViewer.update(entries.get(selectedIndex), null);
                }
            }
        });
    }

    // ---- Entry operations ----

    private void onAdd() {
        commitCurrentEditor();
        BodyEntry newEntry = new BodyEntry("CPP", "");
        entries.add(newEntry);
        entryViewer.refresh();
        entryViewer.setSelection(new StructuredSelection(newEntry), true);
        loadEntry(entries.size() - 1);
    }

    private void onRemove() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return;
        entries.remove(selectedIndex);
        entryViewer.refresh();
        if (entries.isEmpty()) {
            selectedIndex = -1;
            suppressListener = true;
            codeText.setText("");
            languageCombo.setText("");
            suppressListener = false;
        } else {
            int newIdx = Math.min(selectedIndex, entries.size() - 1);
            entryViewer.getTable().select(newIdx);
            loadEntry(newIdx);
        }
    }

    private void onMove(int direction) {
        if (selectedIndex < 0) return;
        int target = selectedIndex + direction;
        if (target < 0 || target >= entries.size()) return;
        commitCurrentEditor();
        BodyEntry entry = entries.remove(selectedIndex);
        entries.add(target, entry);
        selectedIndex = target;
        entryViewer.refresh();
        entryViewer.getTable().select(target);
        updateButtonStates();
    }

    // ---- Sync ----

    private void commitCurrentEditor() {
        if (selectedIndex >= 0 && selectedIndex < entries.size() && codeText != null) {
            entries.get(selectedIndex).body = codeText.getText();
        }
    }

    private void loadEntry(int index) {
        selectedIndex = index;
        if (index < 0 || index >= entries.size()) return;
        BodyEntry entry = entries.get(index);
        
        editorConfigurator.updateSyntaxLanguage(entry.language, codeText);
        if (completionProvider != null) completionProvider.setLanguage(entry.language);

        suppressListener = true;
        try {
            sourceViewer.getDocument().set(entry.body);
            languageCombo.setText(entry.language);
        } finally {
            suppressListener = false;
        }
        
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSel = selectedIndex >= 0;
        removeButton.setEnabled(hasSel);
        upButton.setEnabled(hasSel && selectedIndex > 0);
        downButton.setEnabled(hasSel && selectedIndex < entries.size() - 1);
    }
}
