package com.burhankhanzada.opaquebehavioureditor.editor.highlighting;

import java.util.List;

import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;
import com.burhankhanzada.opaquebehavioureditor.model.TextRange;

public class SemanticPresentationListener implements ITextPresentationListener {

    private final StyledText codeText;
    private final SemanticHighlighter semanticHighlighter;
    private final ModelValidator modelValidator;
    private final EditorThemeManager themeManager;

    public SemanticPresentationListener(StyledText codeText, SemanticHighlighter semanticHighlighter, 
                                        ModelValidator modelValidator, EditorThemeManager themeManager) {
        this.codeText = codeText;
        this.semanticHighlighter = semanticHighlighter;
        this.modelValidator = modelValidator;
        this.themeManager = themeManager;
    }

    @Override
    public void applyTextPresentation(TextPresentation textPresentation) {
        String language = (String) codeText.getData("currentLanguage");
        if (language == null) language = "";
        LanguageDef langDef = LanguageMapping.getLanguageDef(language);
        
        SemanticHighlighter.HighlightingContext ctx = semanticHighlighter.createContext(codeText.getText(), langDef);
        
        // Type Highlighting
        List<TextRange> typeRanges = semanticHighlighter.getUMLTypeRanges(ctx);
        for (TextRange tr : typeRanges) {
            StyleRange style = new StyleRange(tr.offset, tr.length, themeManager.getUmlTypeColor(), null);
            textPresentation.mergeStyleRange(style);
        }
        
        // Method Highlighting
        List<TextRange> methodRanges = semanticHighlighter.getMethodRanges(ctx);
        for (TextRange mr : methodRanges) {
            StyleRange style = new StyleRange(mr.offset, mr.length, themeManager.getMethodColor(), null);
            textPresentation.mergeStyleRange(style);
        }
        
        // Keyword Highlighting
        List<TextRange> keywordRanges = semanticHighlighter.getKeywordRanges(ctx);
        for (TextRange kr : keywordRanges) {
            StyleRange style = new StyleRange(kr.offset, kr.length, themeManager.getKeywordColor(), null);
            textPresentation.mergeStyleRange(style);
        }
        
        // Variable Highlighting
        List<TextRange> varRanges = semanticHighlighter.getVariableRanges(ctx);
        for (TextRange vr : varRanges) {
            StyleRange style = new StyleRange(vr.offset, vr.length, themeManager.getVariableColor(), null);
            textPresentation.mergeStyleRange(style);
        }
        
        // String Highlighting
        List<TextRange> strRanges = semanticHighlighter.getStringRanges(codeText.getText());
        for (TextRange sr : strRanges) {
            StyleRange style = new StyleRange(sr.offset, sr.length, themeManager.getStringColor(), null);
            textPresentation.mergeStyleRange(style);
        }
        
        // Comment Highlighting (Overrides everything else inside the comment)
        List<TextRange> commentRanges = semanticHighlighter.getCommentRanges(codeText.getText());
        for (TextRange cr : commentRanges) {
            StyleRange style = new StyleRange(cr.offset, cr.length, themeManager.getCommentColor(), null);
            textPresentation.mergeStyleRange(style);
        }
        
        // Search Highlighting
        String currentSearchText = themeManager.getCurrentSearchText();
        if (currentSearchText != null && !currentSearchText.isEmpty()) {
            String fullText = codeText.getText();
            String lowerSearch = currentSearchText.toLowerCase();
            String lowerFull = fullText.toLowerCase();
            int idx = lowerFull.indexOf(lowerSearch);
            while (idx != -1) {
                StyleRange sr = new StyleRange(idx, currentSearchText.length(), null, themeManager.getSearchHighlightColor());
                textPresentation.mergeStyleRange(sr);
                idx = lowerFull.indexOf(lowerSearch, idx + currentSearchText.length());
            }
        }
        
        // Errors
        List<TextRange> errors = modelValidator.validateMemberAccess(codeText.getText(), langDef);
        errors.addAll(modelValidator.validateSyntax(codeText.getText(), langDef));
        
        for (TextRange err : errors) {
            StyleRange style = new StyleRange(err.offset, err.length, null, null);
            style.underline = true;
            style.underlineStyle = SWT.UNDERLINE_ERROR;
            style.underlineColor = codeText.getDisplay().getSystemColor(SWT.COLOR_RED);
            textPresentation.mergeStyleRange(style);
        }
    }
}
