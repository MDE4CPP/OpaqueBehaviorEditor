package com.burhankhanzada.opaquebehavioureditor.editor.quickfix;

import java.util.List;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;
import com.burhankhanzada.opaquebehavioureditor.model.TextRange;

public class QuickFixAction {

    private final SourceViewer sourceViewer;
    private final ModelValidator modelValidator;
    private final QuickFixEngine engine;
    private final QuickFixPopup popup;

    public QuickFixAction(SourceViewer sourceViewer, ModelValidator modelValidator) {
        this.sourceViewer = sourceViewer;
        this.modelValidator = modelValidator;
        this.engine = new QuickFixEngine(modelValidator.getDictionary());
        this.popup = new QuickFixPopup(sourceViewer.getTextWidget());
    }

    public void execute() {
        StyledText styledText = sourceViewer.getTextWidget();
        if (styledText == null || styledText.isDisposed()) return;

        int caretOffset = styledText.getCaretOffset();
        String fullText = styledText.getText();

        String language = (String) styledText.getData("currentLanguage");
        if (language == null) language = "";
        LanguageDef langDef = LanguageMapping.getLanguageDef(language);

        // 1. Run validation to find errors
        List<TextRange> errors = modelValidator.validateMemberAccess(fullText, langDef);

        // 2. Find the error that intersects with or is adjacent to the caret
        TextRange targetError = null;
        for (TextRange err : errors) {
            if (caretOffset >= err.offset && caretOffset <= err.offset + err.length) {
                targetError = err;
                break;
            }
        }

        if (targetError == null) return;

        // 3. Extract method and type from error message
        String undefinedMethod = QuickFixEngine.extractMethodFromErrorMessage(targetError.message);
        String typeName = QuickFixEngine.extractTypeFromErrorMessage(targetError.message);

        if (undefinedMethod != null && typeName != null) {
            // 4. Get suggestions
            List<QuickFixEngine.Suggestion> suggestions = engine.suggest(undefinedMethod, typeName);

            // 5. Show popup
            if (!suggestions.isEmpty()) {
                popup.show(targetError.message, suggestions, targetError.offset, targetError.length);
            }
        }
    }
    
    public QuickFixPopup getPopup() {
        return popup;
    }
}
