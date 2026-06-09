package com.burhankhanzada.opaquebehavioureditor.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;

import com.burhankhanzada.opaquebehavioureditor.model.UmlModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.UmlModelValidator;
import com.burhankhanzada.opaquebehavioureditor.editor.LanguageMapping.LanguageDef;

/**
 * Provides code completion for a {@link StyledText} widget.
 * <p>
 * Shows a popup with keyword, type, and snippet suggestions that match
 * the word currently being typed. Triggers:
 * <ul>
 *   <li><b>Automatic</b>: after typing 2+ identifier characters</li>
 *   <li><b>Manual</b>: on Ctrl+Space</li>
 * </ul>
 * Keyboard: ↑↓ to navigate, Enter/Tab to accept, Escape to dismiss.
 */
public class CodeCompletionProvider {

    private final StyledText styledText;
    private Shell popupShell;
    private Table proposalTable;
    private final boolean darkTheme;

    private TreeSet<String> completionWords = new TreeSet<>();
    private LanguageDef currentLangDef;
    private final UmlModelDictionary dictionary;
    private ISelectionProvider selectionProvider;

    /** Tracks whether we're currently inserting a completion (to avoid re-triggering). */
    private boolean inserting = false;

    /** Minimum prefix length before auto-popup triggers. */
    private static final int AUTO_TRIGGER_LENGTH = 2;

    /** Maximum number of proposals shown. */
    private static final int MAX_PROPOSALS = 15;
    

    public CodeCompletionProvider(StyledText styledText, String language, UmlModelDictionary dictionary) {
        this.styledText = styledText;
        this.darkTheme = isDarkTheme(styledText.getDisplay());
        this.dictionary = dictionary;
        setLanguage(language);
        attachListeners();
    }

    /** Update the completion word list when the language changes. */
    public void setLanguage(String language) {
        this.currentLangDef = LanguageMapping.getLanguageDef(language);
        rebuildCompletionWords();
    }

    public void setHyperlinkElements(ISelectionProvider provider) {
        this.selectionProvider = provider;
    }

    private void rebuildCompletionWords() {
        completionWords.clear();
        if (currentLangDef != null && !currentLangDef.isPlainText()) {
            for (String kw : currentLangDef.keywords) completionWords.add(kw);
            for (String tp : currentLangDef.types)    completionWords.add(tp);
        }
        completionWords.addAll(dictionary.autocompleteWords);
        dismissPopup();
    }

    /** Clean up resources. */
    public void dispose() {
        dismissPopup();
    }

    // ------------------------------------------------------------------
    // Listener setup
    // ------------------------------------------------------------------

    private void attachListeners() {
        setupKeyboardTriggers();
        setupAutoPopupTriggers();
        setupFocusDismissal();
        setupSmartPointers();
        setupHoverTooltips();
        setupHyperlinkNavigation();
    }

    private void setupKeyboardTriggers() {
        styledText.addVerifyKeyListener(e -> {
            // 1. Manual trigger (Ctrl+Space)
            // Note: On Mac, Ctrl+Space might be intercepted by OS. 
            if ((e.stateMask & SWT.CTRL) != 0 && (e.character == ' ' || e.keyCode == 32 || e.keyCode == SWT.SPACE)) {
                e.doit = false;
                showCompletions(true);
                return;
            }

            // 2. Navigation when popup is visible
            if (isPopupVisible()) {
                switch (e.keyCode) {
                    case SWT.ARROW_DOWN:
                        navigatePopup(1);
                        e.doit = false;
                        return;
                    case SWT.ARROW_UP:
                        navigatePopup(-1);
                        e.doit = false;
                        return;
                    case SWT.CR:
                    case SWT.KEYPAD_CR:
                    case SWT.TAB:
                        acceptSelected();
                        e.doit = false;
                        return;
                    case SWT.ESC:
                        dismissPopup();
                        e.doit = false;
                        return;
                }
            }
        });
    }

    private void setupAutoPopupTriggers() {
        // ---- Auto-trigger on typing ----
        styledText.addModifyListener(e -> {
            if (inserting) return;
            String prefix = getCurrentPrefix();
            boolean isMemberAccess = isMemberAccessContext();
            
            if (prefix.length() >= AUTO_TRIGGER_LENGTH || isMemberAccess) {
                showCompletions(false);
            } else {
                dismissPopup();
            }
        });
    }

    private void setupFocusDismissal() {
        // ---- Dismiss on focus loss ----
        styledText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // Small delay to allow click on the popup itself
                styledText.getDisplay().timerExec(200, () -> {
                    if (popupShell != null && !popupShell.isDisposed() && !popupShell.isFocusControl()) {
                        dismissPopup();
                    }
                });
            }
        });
    }

    private void setupSmartPointers() {
        // ---- MDE4CPP Smart Pointers: auto '.' to '->' ----
        styledText.addVerifyListener(e -> {
            if (inserting) return;
            if (e.text.equals(".") && currentLangDef != null && currentLangDef.name.equals("CPP")) {
                String textBefore = styledText.getText().substring(0, e.start) + ".";
                String type = CppExpressionParser.resolveContextTypeFromText(textBefore, dictionary, styledText.getText());
                if (type != null) {
                    e.text = "->";
                }
            }
        });
    }

    private void setupHoverTooltips() {
        // ---- Hyperlink and Tooltip logic ----
        styledText.addMouseMoveListener(e -> {
            boolean isMod = (e.stateMask & SWT.MOD1) != 0;
            boolean hasHyperlink = false;
            try {
                int offset = styledText.getOffsetAtPoint(new Point(e.x, e.y));
                String text = styledText.getText();
                int start = offset;
                int end = offset;
                while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) start--;
                while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) end++;
                if (start < end) {
                    String word = text.substring(start, end);
                    String textBefore = text.substring(0, start).stripTrailing();
                    
                    EObject hyperlinkObj = resolveHyperlink(word, textBefore);
                    if (hyperlinkObj != null) {
                        hasHyperlink = true;
                    }

                    String type = resolveVariableType(word);
                    if (type != null && currentLangDef != null && currentLangDef.name.equals("CPP")) {
                        styledText.setToolTipText("std::shared_ptr<" + type + ">");
                    } else if (type != null) {
                        styledText.setToolTipText(type);
                    } else {
                        styledText.setToolTipText(null);
                    }
                } else {
                    styledText.setToolTipText(null);
                }
            } catch (IllegalArgumentException ex) {
                styledText.setToolTipText(null);
            }

            if (isMod && hasHyperlink) {
                styledText.setCursor(styledText.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            } else {
                styledText.setCursor(null);
            }
        });
    }

    private void setupHyperlinkNavigation() {
        styledText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if ((e.keyCode & SWT.MODIFIER_MASK) == SWT.MOD1 || e.keyCode == SWT.COMMAND || e.keyCode == SWT.CTRL) {
                    styledText.setCursor(null);
                }
            }
        });

        styledText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if ((e.stateMask & SWT.MOD1) != 0 && selectionProvider != null) {
                    try {
                        int offset = styledText.getOffsetAtPoint(new Point(e.x, e.y));
                        String text = styledText.getText();
                        int start = offset;
                        int end = offset;
                        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) start--;
                        while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) end++;
                        if (start < end) {
                            String word = text.substring(start, end);
                            String textBefore = text.substring(0, start).stripTrailing();
                            EObject obj = resolveHyperlink(word, textBefore);
                            if (obj != null) {
                                selectionProvider.setSelection(new StructuredSelection(obj));
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        // ignore
                    }
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Completion logic
    // ------------------------------------------------------------------

    private void showCompletions(boolean explicit) {
        String prefix = getCurrentPrefix();
        boolean isMemberAccess = isMemberAccessContext();
        
        if (prefix.isEmpty() && !explicit && !isMemberAccess) {
            dismissPopup();
            return;
        }

        List<String> matches = findMatches(prefix);
        if (matches.isEmpty()) {
            dismissPopup();
            return;
        }

        // Don't show popup if the only match IS the prefix (already complete)
        if (matches.size() == 1 && matches.get(0).equals(prefix)) {
            dismissPopup();
            return;
        }

        showPopup(matches);
    }

    /**
     * Filters the global dictionary and document words to find matches that start
     * with the current prefix. Also handles "member access" context (e.g. `obj->` or `obj.`)
     * by extracting the type of `obj` and only returning its properties and operations.
     * 
     * @param prefix the text typed so far (e.g. "crea")
     * @return a list of suggestions up to MAX_PROPOSALS
     */
    private List<String> findMatches(String prefix) {
        List<String> matches = new ArrayList<>();
        String lower = prefix.toLowerCase();

        boolean isMemberAccess = isMemberAccessContext();
        Set<String> allowedMembers = null;
        
        // Add Snippets at the very top of the list!
        if (currentLangDef != null && currentLangDef.name.equals("CPP") && !isMemberAccess) {
            for (SnippetLibrary.Snippet snip : SnippetLibrary.SNIPPETS) {
                if (snip.keyword.toLowerCase().startsWith(lower)) {
                    if (!matches.contains(snip.label)) {
                        matches.add(snip.label);
                    }
                }
            }
        }
        
        if (isMemberAccess) {
            allowedMembers = new TreeSet<>();
            
            // By default, allow ANY member from any type as a fallback
            for (Map<String, String> members : dictionary.typeMembers.values()) {
                allowedMembers.addAll(members.keySet());
            }
            if (currentLangDef != null && currentLangDef.name.equals("CPP")) {
                for (String m : UmlModelValidator.COMMON_METHODS) allowedMembers.add(m);
            }
            
            // If we can resolve the exact type of the object we're calling a method on,
            // restrict the allowed members exclusively to that type.
            String contextType = resolveContextType();
            if (contextType != null) {
                // Special handling for MDE4CPP collections (Bag, Set, Sequence, etc.)
                if (contextType.startsWith("Bag<") || contextType.startsWith("Set<") || 
                    contextType.startsWith("OrderedSet<") || contextType.startsWith("Sequence<") ||
                    contextType.startsWith("Union<") || contextType.startsWith("SubsetUnion<")) {
                    allowedMembers.clear(); // Only suggest collection methods
                    for (String m : UmlModelValidator.MDE4CPP_COLLECTION_METHODS) allowedMembers.add(m);
                } else if (dictionary.typeMembers.containsKey(contextType)) {
                    // Exact type found, replace fallback with specific members
                    allowedMembers = new TreeSet<>(dictionary.typeMembers.get(contextType).keySet());
                }
            }
        }

        for (String word : completionWords) {
            if (word.toLowerCase().startsWith(lower)) {
                if (allowedMembers != null && !allowedMembers.contains(word) && !word.startsWith("create")) continue;
                if (!isMemberAccess && word.startsWith("create")) continue;
                if (!matches.contains(word)) {
                    matches.add(word);
                }
                if (matches.size() >= MAX_PROPOSALS) break;
            }
        }
        
        // Also harvest words from the document dynamically
        if (matches.size() < MAX_PROPOSALS) {
            String[] docWords = styledText.getText().split("[^a-zA-Z0-9_]+");
            for (String dw : docWords) {
                if (dw.length() >= AUTO_TRIGGER_LENGTH && dw.toLowerCase().startsWith(lower) && !matches.contains(dw)) {
                    if (allowedMembers != null && !allowedMembers.contains(dw) && !dw.startsWith("create")) continue;
                    if (!isMemberAccess && dw.startsWith("create")) continue;
                    matches.add(dw);
                    if (matches.size() >= MAX_PROPOSALS) break;
                }
            }
        }
        
        // If it's a member access but we didn't find enough matches, inject the allowed members directly
        if (isMemberAccess && matches.size() < MAX_PROPOSALS && allowedMembers != null) {
            for (String am : allowedMembers) {
                if (am.toLowerCase().startsWith(lower) && !matches.contains(am)) {
                    matches.add(am);
                    if (matches.size() >= MAX_PROPOSALS) break;
                }
            }
        }
        
        return matches;
    }
    
    /**
     * Checks if the cursor is immediately after a member access token (`.` or `->`).
     * This tells the auto-completer to only suggest properties or operations.
     */
    private boolean isMemberAccessContext() {
        int caretOffset = styledText.getCaretOffset();
        String text = styledText.getText();
        int start = caretOffset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        int ptr = start - 1;
        while (ptr >= 0 && Character.isWhitespace(text.charAt(ptr))) ptr--;
        
        if (ptr >= 0 && text.charAt(ptr) == '.') return true;
        if (ptr >= 1 && text.charAt(ptr) == '>' && text.charAt(ptr-1) == '-') return true;
        return false;
    }

    private String resolveContextType() {
        int caretOffset = styledText.getCaretOffset();
        String text = styledText.getText();
        int start = caretOffset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        String textBeforeCaret = text.substring(0, start).stripTrailing();
        return CppExpressionParser.resolveContextTypeFromText(textBeforeCaret, dictionary, text);
    }

    private EObject resolveHyperlink(String word, String textBeforeCaret) {
        String contextType = CppExpressionParser.resolveContextTypeFromText(textBeforeCaret, dictionary, styledText.getText());
        if (contextType != null) {
            if (contextType.startsWith("std::shared_ptr<")) {
                contextType = contextType.substring(16, contextType.length() - 1);
            }
            if (dictionary.classElements.containsKey(contextType)) {
                return dictionary.classElements.get(contextType).get(word);
            }
        } else {
            return dictionary.globalElements.get(word);
        }
        return null;
    }

    private String resolveVariableType(String variableName) {
        return CppExpressionParser.resolveVariableType(variableName, styledText.getText());
    }

    /** Extracts the identifier being typed at the current caret position. */
    private String getCurrentPrefix() {
        int caretOffset = styledText.getCaretOffset();
        String text = styledText.getText();
        int start = caretOffset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        if (start == caretOffset) return "";
        return text.substring(start, caretOffset);
    }

    /** Returns the offset where the current prefix starts. */
    private int getPrefixStart() {
        int caretOffset = styledText.getCaretOffset();
        String text = styledText.getText();
        int start = caretOffset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        return start;
    }

    // ------------------------------------------------------------------
    // Popup management
    // ------------------------------------------------------------------

    private void showPopup(List<String> proposals) {
        if (popupShell == null || popupShell.isDisposed()) {
            createPopup();
        }

        proposalTable.removeAll();
        for (String p : proposals) {
            TableItem item = new TableItem(proposalTable, SWT.NONE);
            item.setText(p);
        }

        // Position below the current word
        Point caretLocation = styledText.getCaret().getLocation();
        Point displayPoint = styledText.toDisplay(caretLocation.x, caretLocation.y + styledText.getLineHeight());
        popupShell.setLocation(displayPoint);
        popupShell.setSize(280, Math.min(proposals.size() * 22 + 6, MAX_PROPOSALS * 22 + 6));

        if (!popupShell.isVisible()) {
            popupShell.setVisible(true);
        }

        // Select first item
        if (proposalTable.getItemCount() > 0) {
            proposalTable.select(0);
        }
    }

    private void createPopup() {
        popupShell = new Shell(styledText.getShell(), SWT.NO_TRIM | SWT.ON_TOP);
        popupShell.setLayout(new FillLayout());

        proposalTable = new Table(popupShell, SWT.SINGLE | SWT.FULL_SELECTION);
        proposalTable.setFont(styledText.getFont());

        if (darkTheme) {
            proposalTable.setBackground(new Color(new RGB(37, 37, 38)));
            proposalTable.setForeground(new Color(new RGB(212, 212, 212)));
        }

        // Double-click to accept
        proposalTable.addListener(SWT.DefaultSelection, e -> acceptSelected());

        // Single-click to accept
        proposalTable.addListener(SWT.Selection, e -> {
            // Delay so the selection registers first
            styledText.getDisplay().timerExec(50, this::acceptSelected);
        });
    }

    private void dismissPopup() {
        if (popupShell != null && !popupShell.isDisposed()) {
            popupShell.setVisible(false);
        }
    }

    private boolean isPopupVisible() {
        return popupShell != null && !popupShell.isDisposed() && popupShell.isVisible();
    }

    private void navigatePopup(int direction) {
        if (!isPopupVisible()) return;
        int count = proposalTable.getItemCount();
        if (count == 0) return;
        int current = proposalTable.getSelectionIndex();
        int next = Math.max(0, Math.min(count - 1, current + direction));
        proposalTable.select(next);
    }

    private void acceptSelected() {
        if (!isPopupVisible()) return;
        int idx = proposalTable.getSelectionIndex();
        if (idx < 0) {
            dismissPopup();
            return;
        }

        String selected = proposalTable.getItem(idx).getText();
        int prefixStart = getPrefixStart();
        int caretOffset = styledText.getCaretOffset();

        inserting = true;
        try {
            SnippetLibrary.Snippet matchedSnippet = null;
            if (currentLangDef != null && currentLangDef.name.equals("CPP")) {
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

        dismissPopup();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean isDarkTheme(Display display) {
        Color bg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        double brightness = (bg.getRed() * 299.0 + bg.getGreen() * 587.0 + bg.getBlue() * 114.0) / 1000.0;
        return brightness < 128;
    }
}
