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
        "Bag", "Set", "OrderedSet", "Sequence", "Union", "SubsetUnion",
        "Any", "const_iterator", "iterator", "string", "bool", "int", "double", "float", "char", "void"
    );

    public static final Set<String> KEYWORDS = Set.of(
        "if", "else", "for", "while", "do", "return", "new", "delete", "const", "auto", "this", 
        "class", "struct", "public", "private", "protected", "virtual", "override", "switch", "case", 
        "break", "continue", "true", "false", "nullptr"
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

    public List<TextRange> getKeywordRanges(String text, LanguageDef currentLangDef) {
        List<TextRange> ranges = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP")) return ranges;
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_]+)\\b");
        java.util.regex.Matcher m = p.matcher(text);
        
        while (m.find()) {
            String word = m.group(1);
            if (KEYWORDS.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }

    public List<TextRange> getVariableRanges(String text, LanguageDef currentLangDef) {
        List<TextRange> ranges = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP")) return ranges;
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_]+)\\b");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            String word = m.group(1);
            if (Character.isDigit(word.charAt(0))) continue; // Skip numbers
            
            // If it is NOT a Type, NOT a Keyword, and NOT followed by a '(', it is a variable
            if (!dictionary.typeMembers.containsKey(word) && !STD_TYPES.contains(word) && !KEYWORDS.contains(word)) {
                int nextIndex = m.end(1);
                boolean isMethod = false;
                while (nextIndex < text.length() && Character.isWhitespace(text.charAt(nextIndex))) {
                    nextIndex++;
                }
                if (nextIndex < text.length() && text.charAt(nextIndex) == '(') {
                    isMethod = true;
                }
                
                if (!isMethod) {
                    ranges.add(new TextRange(m.start(1), word.length(), null));
                }
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
