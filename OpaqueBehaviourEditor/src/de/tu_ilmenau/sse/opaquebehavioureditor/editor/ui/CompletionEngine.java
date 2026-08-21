package de.tu_ilmenau.sse.opaquebehavioureditor.editor.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.ExpressionParser;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageDef;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageMapping;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.SnippetLibrary;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.AbstractModelHarvester;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelValidator;

public class CompletionEngine {

    private final ModelDictionary dictionary;
    private LanguageDef currentLangDef;
    private static final int AUTO_TRIGGER_LENGTH = 2;
    public static final int MAX_PROPOSALS = 50;
    public static final int MAX_VISIBLE_ROWS = 15;

    public CompletionEngine(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public void setLanguage(String language) {
        this.currentLangDef = LanguageMapping.getLanguageDef(language);
    }

    /**
     * The language's own words plus everything harvested from the model. Read on each query
     * rather than cached, because the dictionary is filled after this engine is constructed.
     */
    private Set<String> completionWords() {
        Set<String> words = new TreeSet<>();
        if (currentLangDef != null && !currentLangDef.isPlainText()) {
            for (String kw : currentLangDef.keywords) words.add(kw);
            for (String tp : currentLangDef.types) words.add(tp);
        }
        words.addAll(dictionary.getAutocompleteWords());
        return words;
    }

    public LanguageDef getCurrentLangDef() {
        return currentLangDef;
    }

    public List<String> findMatches(String prefix, boolean isMemberAccess, String contextType, String documentText) {
        return findMatches(prefix, isMemberAccess, contextType, documentText,
                documentText == null ? 0 : documentText.length());
    }

    public List<String> findMatches(String prefix, boolean isMemberAccess, String contextType,
            String documentText, int caretOffset) {
        List<String> matches = new ArrayList<>();
        String lower = prefix.toLowerCase();

        Set<String> allowedMembers = null;
        
        // Add Snippets at the very top of the list!
        if (currentLangDef != null && currentLangDef.name.equals(LanguageMapping.LANG_CPP) && !isMemberAccess) {
            for (SnippetLibrary.Snippet snip : SnippetLibrary.getSnippets()) {
                if (snip.keyword.toLowerCase().startsWith(lower)) {
                    if (!matches.contains(snip.label)) {
                        matches.add(snip.label);
                    }
                }
            }
        }
        
        if (isMemberAccess) {
            allowedMembers = new TreeSet<>();
            
            // By default, allow ANY member from any type as a fallback.
            // The factory's create* methods are excluded - they are only valid on `factory->`.
            for (Map.Entry<String, Map<String, String>> e : dictionary.getTypeMembers().entrySet()) {
                if (AbstractModelHarvester.FACTORY_TYPE.equals(e.getKey())) continue;
                allowedMembers.addAll(e.getValue().keySet());
            }
            if (currentLangDef != null && currentLangDef.name.equals(LanguageMapping.LANG_CPP)) {
                for (String m : ModelValidator.COMMON_METHODS) allowedMembers.add(m);
            }
            
            // If we can resolve the exact type of the object we're calling a method on,
            // restrict the allowed members exclusively to that type.
            if (contextType != null) {
                // Special handling for MDE4CPP collections (Bag, Union, Subset, SubsetUnion)
                if (ModelValidator.isCollectionType(contextType)) {
                    allowedMembers.clear(); // Only suggest collection methods
                    for (String m : ModelValidator.MDE4CPP_COLLECTION_METHODS) allowedMembers.add(m);
                } else if (dictionary.getTypeMembers().containsKey(contextType) || !dictionary.getAllTypeMembers(contextType).isEmpty()) {
                    // It's a regular known type — include inherited members from supertypes
                    allowedMembers = new TreeSet<>(dictionary.getAllTypeMembers(contextType).keySet());
                }
            }
        }

        // Collect every candidate first, then rank - truncating an alphabetical scan would cut
        // the model's own accessors in favour of whatever sorts early.
        Set<String> candidates = new LinkedHashSet<>();

        for (String word : completionWords()) {
            if (word.toLowerCase().startsWith(lower)) {
                if (allowedMembers != null && !allowedMembers.contains(word)) continue;
                if (!isMemberAccess && word.startsWith("create")) continue;
                candidates.add(word);
            }
        }

        // Also harvest words from the document dynamically
        for (String dw : documentText.split("[^a-zA-Z0-9_]+")) {
            if (dw.length() < AUTO_TRIGGER_LENGTH || !dw.toLowerCase().startsWith(lower)) continue;
            if (allowedMembers != null && !allowedMembers.contains(dw)) continue;
            if (!isMemberAccess && dw.startsWith("create")) continue;
            candidates.add(dw);
        }

        // Members that are known for the type but were never registered as a global word
        if (isMemberAccess && allowedMembers != null) {
            for (String am : allowedMembers) {
                if (am.toLowerCase().startsWith(lower)) candidates.add(am);
            }
        }

        // Outside a member access the body's own variables are what the user reaches for first.
        Set<String> variables = isMemberAccess
                ? Set.of()
                : ExpressionParser.declaredVariables(documentText, caretOffset);

        List<String> ranked = new ArrayList<>(candidates);
        ranked.sort(Comparator.comparingInt((String w) -> rank(w, isMemberAccess, contextType, variables))
                              .thenComparing(Comparator.naturalOrder()));

        for (String word : ranked) {
            if (matches.size() >= MAX_PROPOSALS) break;
            // The word being typed is picked up by the document scan; completing to it is a no-op.
            if (word.equals(prefix)) continue;
            if (!matches.contains(word)) matches.add(word);
        }
        return matches;
    }

    /** Collection methods that come up most in hand-written bodies, most used first. */
    private static final List<String> COLLECTION_BY_USAGE =
            List.of("add", "size", "at", "front", "empty", "push_back", "erase", "clear");

    /**
     * Orders proposals by how likely they are to be what the user wants: the type's own getters
     * and setters first, then its other members, then inherited ones, and only then the reflective
     * Ecore/OCL API that every object carries.
     */
    private int rank(String word, boolean isMemberAccess, String contextType, Set<String> variables) {
        if (!isMemberAccess) {
            if (variables.contains(word)) return RANK_VARIABLE;
            if (isConstantName(word)) return RANK_CONSTANT;
            return RANK_PLAIN;
        }
        if (contextType == null) return RANK_PLAIN;

        if (ModelValidator.isCollectionType(contextType)) {
            int used = COLLECTION_BY_USAGE.indexOf(word);
            return used >= 0 ? used : COLLECTION_BY_USAGE.size();
        }

        String owner = dictionary.getDeclaringType(contextType, word);
        if (owner == null) return RANK_PLAIN;

        boolean accessor = word.startsWith("get") || word.startsWith("set");
        // Metamodel and service types are qualified (ecore::EObject, aql::Services); a model
        // class from the harvested .ecore or .uml is always a simple name.
        boolean reflective = owner.contains("::");
        if (reflective) return accessor ? RANK_REFLECTIVE_ACCESSOR : RANK_REFLECTIVE;
        if (owner.equals(contextType)) return accessor ? RANK_OWN_ACCESSOR : RANK_OWN;
        return accessor ? RANK_INHERITED_ACCESSOR : RANK_INHERITED;
    }

    /** SCREAMING_SNAKE_CASE, the shape of the generated package constants and of C++ macros. */
    private static boolean isConstantName(String word) {
        return word.length() > 1 && word.equals(word.toUpperCase())
                && Character.isLetter(word.charAt(0));
    }

    private static final int RANK_VARIABLE = 0;
    private static final int RANK_CONSTANT = 1;

    private static final int RANK_OWN_ACCESSOR = 0;
    private static final int RANK_INHERITED_ACCESSOR = 1;
    private static final int RANK_OWN = 2;
    private static final int RANK_INHERITED = 3;
    private static final int RANK_REFLECTIVE_ACCESSOR = 4;
    private static final int RANK_REFLECTIVE = 5;
    private static final int RANK_PLAIN = 6;
}
