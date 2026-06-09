package com.burhankhanzada.opaquebehavioureditor.editor.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.SnippetLibrary;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;

public class CompletionEngine {

    private final ModelDictionary dictionary;
    private LanguageDef currentLangDef;
    private final TreeSet<String> completionWords = new TreeSet<>();
    private static final int AUTO_TRIGGER_LENGTH = 2;
    public static final int MAX_PROPOSALS = 15;

    public CompletionEngine(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void setLanguage(String language) {
        this.currentLangDef = LanguageMapping.getLanguageDef(language);
        rebuildCompletionWords();
    }

    private void rebuildCompletionWords() {
        completionWords.clear();
        if (currentLangDef != null && !currentLangDef.isPlainText()) {
            for (String kw : currentLangDef.keywords) completionWords.add(kw);
            for (String tp : currentLangDef.types)    completionWords.add(tp);
        }
        completionWords.addAll(dictionary.autocompleteWords);
    }

    public LanguageDef getCurrentLangDef() {
        return currentLangDef;
    }

    public List<String> findMatches(String prefix, boolean isMemberAccess, String contextType, String documentText) {
        List<String> matches = new ArrayList<>();
        String lower = prefix.toLowerCase();

        Set<String> allowedMembers = null;
        
        // Add Snippets at the very top of the list!
        if (currentLangDef != null && currentLangDef.name.equals(LanguageMapping.LANG_CPP) && !isMemberAccess) {
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
            if (currentLangDef != null && currentLangDef.name.equals(LanguageMapping.LANG_CPP)) {
                for (String m : ModelValidator.COMMON_METHODS) allowedMembers.add(m);
            }
            
            // If we can resolve the exact type of the object we're calling a method on,
            // restrict the allowed members exclusively to that type.
            if (contextType != null) {
                // Special handling for MDE4CPP collections (Bag, Set, Sequence, etc.)
                if (contextType.startsWith("Bag<") || contextType.startsWith("Set<") || 
                    contextType.startsWith("OrderedSet<") || contextType.startsWith("Sequence<") ||
                    contextType.startsWith("Union<") || contextType.startsWith("SubsetUnion<")) {
                    allowedMembers.clear(); // Only suggest collection methods
                    for (String m : ModelValidator.MDE4CPP_COLLECTION_METHODS) allowedMembers.add(m);
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
            String[] docWords = documentText.split("[^a-zA-Z0-9_]+");
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
}
