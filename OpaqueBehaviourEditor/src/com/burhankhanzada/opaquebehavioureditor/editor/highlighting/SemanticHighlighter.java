package com.burhankhanzada.opaquebehavioureditor.editor.highlighting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.burhankhanzada.opaquebehavioureditor.model.TextRange;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;

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

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b([A-Za-z0-9_]+)\\b");
    private static final Pattern METHOD_CALL_PATTERN = Pattern.compile("\\b([A-Za-z0-9_]+)\\s*\\(");

    private final ModelDictionary dictionary;

    public SemanticHighlighter(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<TextRange> getUMLTypeRanges(HighlightingContext ctx) {
        List<TextRange> ranges = new ArrayList<>();
        if (ctx.lang() == null || !ctx.lang().name.equals(LanguageMapping.LANG_CPP) || dictionary.getTypeMembers().isEmpty()) {
            return ranges;
        }
        
        List<TextRange> ignored = ctx.ignored();
        String text = ctx.text();
        Matcher m = WORD_PATTERN.matcher(text);
        
        while (m.find()) {
            if (isIgnored(m.start(1), ignored)) continue;
            String word = m.group(1);
            if (dictionary.getTypeMembers().containsKey(word) || STD_TYPES.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }

    public List<TextRange> getKeywordRanges(HighlightingContext ctx) {
        List<TextRange> ranges = new ArrayList<>();
        if (ctx.lang() == null || !ctx.lang().name.equals(LanguageMapping.LANG_CPP)) return ranges;
        
        List<TextRange> ignored = ctx.ignored();
        String text = ctx.text();
        Matcher m = WORD_PATTERN.matcher(text);
        
        while (m.find()) {
            if (isIgnored(m.start(1), ignored)) continue;
            String word = m.group(1);
            if (KEYWORDS.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }

    public List<TextRange> getVariableRanges(HighlightingContext ctx) {
        List<TextRange> ranges = new ArrayList<>();
        if (ctx.lang() == null || !ctx.lang().name.equals(LanguageMapping.LANG_CPP)) return ranges;
        
        List<TextRange> ignored = ctx.ignored();
        String text = ctx.text();
        Matcher m = WORD_PATTERN.matcher(text);
        while (m.find()) {
            if (isIgnored(m.start(1), ignored)) continue;
            String word = m.group(1);
            if (Character.isDigit(word.charAt(0))) continue; // Skip numbers
            
            // If it is NOT a Type, NOT a Keyword, and NOT followed by a '(', it is a variable
            if (!dictionary.getTypeMembers().containsKey(word) && !STD_TYPES.contains(word) && !KEYWORDS.contains(word)) {
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

    public List<TextRange> getMethodRanges(HighlightingContext ctx) {
        List<TextRange> ranges = new ArrayList<>();
        if (ctx.lang() == null || !ctx.lang().name.equals(LanguageMapping.LANG_CPP)) return ranges;
        
        List<TextRange> ignored = ctx.ignored();
        String text = ctx.text();
        Matcher m = METHOD_CALL_PATTERN.matcher(text);
        
        while (m.find()) {
            if (isIgnored(m.start(1), ignored)) continue;
            String word = m.group(1);
            // Must not be a keyword like "if (" or "while ("
            if (!KEYWORDS.contains(word)) {
                ranges.add(new TextRange(m.start(1), word.length(), null));
            }
        }
        return ranges;
    }

    public static List<TextRange> getCommentRanges(String text) {
        List<TextRange> comments = new ArrayList<>();
        int i = 0;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < len) {
                if (text.charAt(i + 1) == '/') {
                    int start = i;
                    while (i < len && text.charAt(i) != '\n' && text.charAt(i) != '\r') i++;
                    comments.add(new TextRange(start, i - start, null));
                    continue;
                } else if (text.charAt(i + 1) == '*') {
                    int start = i;
                    i += 2;
                    while (i + 1 < len && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) i++;
                    i += 2; // skip */
                    comments.add(new TextRange(start, Math.min(len, i) - start, null));
                    continue;
                }
            } else if (c == '"' || c == '\'') {
                // skip strings so they aren't marked as comments
                char quote = c;
                i++;
                while (i < len) {
                    if (text.charAt(i) == '\\') i += 2;
                    else if (text.charAt(i) == quote) { i++; break; }
                    else i++;
                }
                continue;
            }
            i++;
        }
        return comments;
    }

    public static List<TextRange> getStringRanges(String text) {
        List<TextRange> strings = new ArrayList<>();
        int i = 0;
        int len = text.length();
        while (i < len) {
            char c = text.charAt(i);
            if (c == '/' && i + 1 < len && (text.charAt(i + 1) == '/' || text.charAt(i + 1) == '*')) {
                // skip comments so they aren't marked as strings
                if (text.charAt(i + 1) == '/') {
                    while (i < len && text.charAt(i) != '\n' && text.charAt(i) != '\r') i++;
                } else {
                    i += 2;
                    while (i + 1 < len && !(text.charAt(i) == '*' && text.charAt(i + 1) == '/')) i++;
                    i += 2;
                }
                continue;
            } else if (c == '"' || c == '\'') {
                int start = i;
                char quote = c;
                i++;
                while (i < len) {
                    if (text.charAt(i) == '\\') i += 2;
                    else if (text.charAt(i) == quote) { i++; break; }
                    else i++;
                }
                strings.add(new TextRange(start, Math.min(len, i) - start, null));
                continue;
            }
            i++;
        }
        return strings;
    }

    public static List<TextRange> getIgnoredRanges(String text) {
        List<TextRange> ignored = new ArrayList<>();
        ignored.addAll(getCommentRanges(text));
        ignored.addAll(getStringRanges(text));
        return ignored;
    }
    
    private boolean isIgnored(int offset, List<TextRange> ignoredRanges) {
        for (TextRange r : ignoredRanges) {
            if (offset >= r.offset && offset < r.offset + r.length) return true;
        }
        return false;
    }
}
