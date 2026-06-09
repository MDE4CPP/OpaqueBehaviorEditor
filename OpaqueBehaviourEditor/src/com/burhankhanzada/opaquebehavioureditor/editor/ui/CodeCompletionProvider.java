package com.burhankhanzada.opaquebehavioureditor.editor.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.jface.viewers.ISelectionProvider;

import com.burhankhanzada.opaquebehavioureditor.editor.text.CppExpressionParser;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.editor.text.SnippetLibrary;
import com.burhankhanzada.opaquebehavioureditor.editor.text.TextUtilities;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;

/**
 * Orchestrates code completion, smart pointers, tooltips, and hyperlink navigation
 * for a {@link StyledText} widget.
 */
public class CodeCompletionProvider {

    private final StyledText styledText;
    private final CompletionEngine engine;
    private final CompletionPopup popup;
    private final EditorInteractionHandler interactionHandler;
    private final ModelDictionary dictionary;

    /** Tracks whether we're currently inserting a completion (to avoid re-triggering). */
    private boolean inserting = false;
    private static final int AUTO_TRIGGER_LENGTH = 2;

    public CodeCompletionProvider(StyledText styledText, String language, ModelDictionary dictionary) {
        this.styledText = styledText;
        this.dictionary = dictionary;
        this.engine = new CompletionEngine(dictionary);
        this.popup = new CompletionPopup(styledText);
        this.interactionHandler = new EditorInteractionHandler(styledText, dictionary);

        setLanguage(language);

        popup.setOnAccept(this::acceptSelected);
        attachListeners();
    }

    public void setLanguage(String language) {
        engine.setLanguage(language);
        interactionHandler.setLanguage(engine.getCurrentLangDef());
    }

    public void setHyperlinkElements(ISelectionProvider provider) {
        interactionHandler.setSelectionProvider(provider);
    }

    public void dispose() {
        popup.dismiss();
    }

    private void attachListeners() {
        setupKeyboardTriggers();
        setupAutoPopupTriggers();
        setupFocusDismissal();
        setupSmartPointers();
    }

    private void setupKeyboardTriggers() {
        styledText.addVerifyKeyListener(e -> {
            if ((e.stateMask & SWT.CTRL) != 0 && (e.character == ' ' || e.keyCode == 32 || e.keyCode == SWT.SPACE)) {
                e.doit = false;
                showCompletions(true);
                return;
            }

            if (popup.isVisible()) {
                switch (e.keyCode) {
                    case SWT.ARROW_DOWN:
                        popup.navigate(1);
                        e.doit = false;
                        return;
                    case SWT.ARROW_UP:
                        popup.navigate(-1);
                        e.doit = false;
                        return;
                    case SWT.CR:
                    case SWT.KEYPAD_CR:
                    case SWT.TAB:
                        acceptSelected();
                        e.doit = false;
                        return;
                    case SWT.ESC:
                        popup.dismiss();
                        e.doit = false;
                        return;
                }
            }
        });
    }

    private void setupAutoPopupTriggers() {
        styledText.addModifyListener(e -> {
            if (inserting) return;
            String prefix = getCurrentPrefix();
            boolean isMemberAccess = isMemberAccessContext();
            
            if (prefix.length() >= AUTO_TRIGGER_LENGTH || isMemberAccess) {
                showCompletions(false);
            } else {
                popup.dismiss();
            }
        });
    }

    private void setupFocusDismissal() {
        styledText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                styledText.getDisplay().timerExec(200, () -> {
                    if (!popup.isFocusControl()) {
                        popup.dismiss();
                    }
                });
            }
        });
    }

    private void setupSmartPointers() {
        styledText.addVerifyListener(e -> {
            if (inserting) return;
            if (e.text.equals(".") && engine.getCurrentLangDef() != null && engine.getCurrentLangDef().name.equals(LanguageMapping.LANG_CPP)) {
                String textBefore = styledText.getText().substring(0, e.start) + ".";
                String type = CppExpressionParser.resolveContextTypeFromText(textBefore, dictionary, styledText.getText());
                if (type != null) {
                    e.text = "->";
                }
            }
        });
    }

    private void showCompletions(boolean explicit) {
        String prefix = getCurrentPrefix();
        boolean isMemberAccess = isMemberAccessContext();
        
        if (prefix.isEmpty() && !explicit && !isMemberAccess) {
            popup.dismiss();
            return;
        }

        String contextType = null;
        if (isMemberAccess) {
            int caretOffset = styledText.getCaretOffset();
            String textBeforeCaret = TextUtilities.getTextBeforeIdentifier(styledText.getText(), caretOffset);
            contextType = CppExpressionParser.resolveContextTypeFromText(textBeforeCaret, dictionary, styledText.getText());
        }

        List<String> matches = engine.findMatches(prefix, isMemberAccess, contextType, styledText.getText());
        
        if (matches.isEmpty()) {
            popup.dismiss();
            return;
        }

        if (matches.size() == 1 && matches.get(0).equals(prefix)) {
            popup.dismiss();
            return;
        }

        popup.show(matches);
    }

    private boolean isMemberAccessContext() {
        int caretOffset = styledText.getCaretOffset();
        String text = styledText.getText();
        int[] bounds = TextUtilities.getWordBounds(text, caretOffset);
        int ptr = bounds[0] - 1;
        while (ptr >= 0 && Character.isWhitespace(text.charAt(ptr))) ptr--;
        
        if (ptr >= 0 && text.charAt(ptr) == '.') return true;
        if (ptr >= 1 && text.charAt(ptr) == '>' && text.charAt(ptr-1) == '-') return true;
        return false;
    }

    private String getCurrentPrefix() {
        int caretOffset = styledText.getCaretOffset();
        String text = styledText.getText();
        int[] bounds = TextUtilities.getWordBounds(text, caretOffset);
        int start = bounds[0];
        if (start == caretOffset) return "";
        return text.substring(start, caretOffset);
    }

    private int getPrefixStart() {
        int caretOffset = styledText.getCaretOffset();
        return TextUtilities.getWordBounds(styledText.getText(), caretOffset)[0];
    }

    private void acceptSelected() {
        if (!popup.isVisible()) return;
        
        String selected = popup.getSelectedProposal();
        if (selected == null) {
            popup.dismiss();
            return;
        }

        int prefixStart = getPrefixStart();
        int caretOffset = styledText.getCaretOffset();

        inserting = true;
        try {
            SnippetLibrary.Snippet matchedSnippet = null;
            if (engine.getCurrentLangDef() != null && engine.getCurrentLangDef().name.equals(LanguageMapping.LANG_CPP)) {
                for (SnippetLibrary.Snippet s : SnippetLibrary.SNIPPETS) {
                    if (s.label.equals(selected)) {
                        matchedSnippet = s;
                        break;
                    }
                }
            }
            if (matchedSnippet != null) {
                styledText.replaceTextRange(prefixStart, caretOffset - prefixStart, matchedSnippet.template);
                int placeholderIndex = matchedSnippet.template.indexOf(matchedSnippet.placeholder);
                if (placeholderIndex >= 0) {
                    styledText.setSelection(prefixStart + placeholderIndex, prefixStart + placeholderIndex + matchedSnippet.placeholder.length());
                } else {
                    styledText.setCaretOffset(prefixStart + matchedSnippet.template.length());
                }
            } else {
                styledText.replaceTextRange(prefixStart, caretOffset - prefixStart, selected);
                styledText.setCaretOffset(prefixStart + selected.length());
            }
        } finally {
            inserting = false;
        }

        popup.dismiss();
    }
}
