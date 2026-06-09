package com.burhankhanzada.opaquebehavioureditor.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.burhankhanzada.opaquebehavioureditor.model.TextRange;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.editor.LanguageMapping.LanguageDef;

public class SemanticHighlighter {

    public static final Set<String> STD_TYPES = Set.of(
        "std", "shared_ptr", "weak_ptr", "unique_ptr", "dynamic_pointer_cast",
        "Bag", "Set", "OrderedSet", "Sequence", "Union", "SubsetUnion"
    );

    private final ModelDictionary dictionary;

    public SemanticHighlighter(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<TextRange> getUMLTypeRanges(String text, LanguageDef currentLangDef) {
        List<TextRange> ranges = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP") || dictionary.typeMembers.isEmpty()) {
            return ranges;
        }
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_]+)\\b");
        java.util.regex.Matcher m = p.matcher(text);
        
        while (m.find()) {
            String word = m.group(1);
            if (dictionary.typeMembers.containsKey(word) || STD_TYPES.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }

    public List<TextRange> getVariableRanges(String text, LanguageDef currentLangDef) {
        List<TextRange> ranges = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP")) return ranges;
        
        Set<String> vars = new HashSet<>();
        vars.add("factory"); // Always highlight factory
        
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("std::(?:weak|shared|unique)_ptr<[^>]+>\\s+([A-Za-z0-9_]+)\\b");
        java.util.regex.Matcher m1 = p1.matcher(text);
        while (m1.find()) vars.add(m1.group(1));
        
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_:]+)\\s*\\**\\s+([A-Za-z0-9_]+)\\s*(?:=|;)");
        java.util.regex.Matcher m2 = p2.matcher(text);
        while (m2.find()) {
            String t = m2.group(1);
            if (!t.equals("return") && !t.equals("new") && !t.equals("delete")) {
                vars.add(m2.group(2));
            }
        }
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_]+)\\b");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            String word = m.group(1);
            if (vars.contains(word) && !dictionary.typeMembers.containsKey(word) && !STD_TYPES.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }

    public List<TextRange> getMethodRanges(String text, LanguageDef currentLangDef) {
        List<TextRange> ranges = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP")) return ranges;
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_]+)\\s*\\(");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            String word = m.group(1);
            if (!dictionary.typeMembers.containsKey(word) && !STD_TYPES.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }
}
