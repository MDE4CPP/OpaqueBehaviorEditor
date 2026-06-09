package com.burhankhanzada.opaquebehavioureditor.ui;

import java.util.function.BiConsumer;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.burhankhanzada.opaquebehavioureditor.StringConstants;
import com.burhankhanzada.opaquebehavioureditor.editor.core.CodeEditorConfigurator;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.SemanticHighlighter;
import com.burhankhanzada.opaquebehavioureditor.editor.text.AutoFormatter;
import com.burhankhanzada.opaquebehavioureditor.editor.text.CodeTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.editor.ui.CodeCompletionProvider;
import com.burhankhanzada.opaquebehavioureditor.model.BodyEntry;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;

public class CodeEditorComposite extends Composite {

    private Combo languageCombo;
    private Combo targetLanguageCombo;
    private SourceViewer sourceViewer;
    private StyledText codeText;
    
    private CodeEditorConfigurator editorConfigurator;
    private CodeCompletionProvider completionProvider;
    
    private boolean suppressListener = false;
    
    private BiConsumer<String, String> changeListener;
    private Runnable saveAction;

    public CodeEditorComposite(Composite parent, int style, ModelDictionary dictionary, ISelectionProvider selectionProvider) {
        super(parent, style);
        
        setLayout(new GridLayout(1, false));
        
        Composite row = new Composite(this, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        row.setLayout(new GridLayout(6, false));

        Label lbl = new Label(row, SWT.NONE);
        lbl.setText(StringConstants.LBL_LANGUAGE);

        languageCombo = new Combo(row, SWT.DROP_DOWN | SWT.READ_ONLY);
        languageCombo.setItems(LanguageMapping.getAllLanguageNames());
        languageCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (ThemeUtils.isDarkTheme(parent)) ThemeUtils.fixComboDarkTheme(languageCombo);
        
        languageCombo.addModifyListener(e -> {
            if (!suppressListener) {
                editorConfigurator.updateSyntaxLanguage(languageCombo.getText(), codeText);
                if (completionProvider != null) completionProvider.setLanguage(languageCombo.getText());
                notifyChange();
            }
        });

        Button formatBtn = new Button(row, SWT.PUSH);
        formatBtn.setText(StringConstants.BTN_FORMAT_CODE);
        formatBtn.addListener(SWT.Selection, e -> {
            if (codeText != null && !codeText.isDisposed()) {
                String formatted = AutoFormatter.format(codeText.getText(), languageCombo.getText());
                codeText.setText(formatted);
            }
        });

        Label transLbl = new Label(row, SWT.NONE);
        transLbl.setText(StringConstants.LBL_TRANSLATE_TO);

        targetLanguageCombo = new Combo(row, SWT.DROP_DOWN | SWT.READ_ONLY);
        targetLanguageCombo.setItems(LanguageMapping.getAllLanguageNames());
        if (targetLanguageCombo.getItemCount() > 0) targetLanguageCombo.select(0);
        if (ThemeUtils.isDarkTheme(parent)) ThemeUtils.fixComboDarkTheme(targetLanguageCombo);

        Button translateBtn = new Button(row, SWT.PUSH);
        translateBtn.setText(StringConstants.BTN_TRANSLATE);
        translateBtn.addListener(SWT.Selection, e -> {
            if (codeText != null && !codeText.isDisposed()) {
                String sourceLang = languageCombo.getText();
                String targetLang = targetLanguageCombo.getText();
                String translated = CodeTranslator.translate(codeText.getText(), sourceLang, targetLang);
                
                String formatted = AutoFormatter.format(translated, targetLang);
                
                codeText.setText(formatted);
                languageCombo.setText(targetLang); 
            }
        });
        Label codeLbl = new Label(this, SWT.NONE);
        codeLbl.setText(StringConstants.LBL_BODY_CODE);

        sourceViewer = new SourceViewer(this, null, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        sourceViewer.setDocument(new Document(""));
        codeText = sourceViewer.getTextWidget();

        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 350;
        gd.widthHint  = 600;
        codeText.setLayoutData(gd);

        SemanticHighlighter semanticHighlighter = new SemanticHighlighter(dictionary);
        ModelValidator modelValidator = new ModelValidator(dictionary);
        this.editorConfigurator = new CodeEditorConfigurator(semanticHighlighter, modelValidator);
        
        editorConfigurator.configure(this, sourceViewer);

        completionProvider = new CodeCompletionProvider(codeText, "", dictionary);
        completionProvider.setHyperlinkElements(selectionProvider);

        codeText.addModifyListener(e -> {
            if (!suppressListener) {
                notifyChange();
            }
        });

        codeText.addVerifyKeyListener(e -> {
            boolean isCtrl = (e.stateMask & SWT.MOD1) != 0;
            if (isCtrl && e.keyCode == 's') {
                if (saveAction != null) {
                    saveAction.run();
                }
                e.doit = false;
            }
        });
    }

    public void setChangeListener(BiConsumer<String, String> listener) {
        this.changeListener = listener;
    }
    
    public void setSaveAction(Runnable saveAction) {
        this.saveAction = saveAction;
    }
    
    private void notifyChange() {
        if (changeListener != null) {
            changeListener.accept(languageCombo.getText(), codeText.getText());
        }
    }

    public void loadEntry(BodyEntry entry) {
        if (entry == null) {
            suppressListener = true;
            try {
                sourceViewer.getDocument().set("");
                languageCombo.setText("");
            } finally {
                suppressListener = false;
            }
            codeText.setEnabled(false);
            languageCombo.setEnabled(false);
            return;
        }
        
        codeText.setEnabled(true);
        languageCombo.setEnabled(true);
        
        editorConfigurator.updateSyntaxLanguage(entry.language, codeText);
        if (completionProvider != null) completionProvider.setLanguage(entry.language);

        suppressListener = true;
        try {
            sourceViewer.getDocument().set(entry.body);
            languageCombo.setText(entry.language);
        } finally {
            suppressListener = false;
        }
    }

    @Override
    public void dispose() {
        if (editorConfigurator != null) {
            editorConfigurator.dispose();
        }
        if (completionProvider != null) {
            completionProvider.dispose();
        }
        super.dispose();
    }
}
