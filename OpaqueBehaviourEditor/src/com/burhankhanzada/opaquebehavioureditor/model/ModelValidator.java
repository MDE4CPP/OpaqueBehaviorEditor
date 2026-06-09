package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.burhankhanzada.opaquebehavioureditor.editor.LanguageMapping.LanguageDef;

public class ModelValidator {

    public static final String[] COMMON_METHODS = {
        "add", "remove", "clear", "size", "empty", "front", "back", "insert", "erase", 
        "push_back", "pop_back", "begin", "end", "find", "count", "length", "substr", "at",
        // EMF standard methods
        "eSet", "eGet", "eIsSet", "eUnset", "eClass", "eContainer", "eContents",
        "eAllContents", "eCrossReferences", "eResource", "eIsProxy", "eResolveProxy"
    };

    public static final String[] MDE4CPP_COLLECTION_METHODS = {
        "add", "insert", "remove", "erase", "clear", "size", "empty", 
        "front", "back", "begin", "end", "at"
    };

    private final ModelDictionary dictionary;

    public ModelValidator(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<TextRange> validateMemberAccess(String text, LanguageDef currentLangDef) {
        List<TextRange> errors = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP")) {
            return errors;
        }
        
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("->[ \\t]*([A-Za-z0-9_]+)");
        java.util.regex.Matcher m = p.matcher(text);
        
        while (m.find()) {
            String methodName = m.group(1);
            int methodOffset = m.start(1);
            int methodLength = methodName.length();
            
            String textBefore = text.substring(0, m.start() + 2);
            String rawType = resolveContextTypeFromText(textBefore);
            
            if (rawType != null) {
                boolean isCollection = rawType.startsWith("Bag<") || rawType.startsWith("Set<") || 
                                       rawType.startsWith("OrderedSet<") || rawType.startsWith("Sequence<") ||
                                       rawType.startsWith("Union<") || rawType.startsWith("SubsetUnion<");
                                       
                if (rawType.startsWith("std::shared_ptr<")) {
                    rawType = rawType.substring(16, rawType.length() - 1);
                }
                
                boolean isValid = false;
                
                if (isCollection) {
                    for (String cm : MDE4CPP_COLLECTION_METHODS) {
                        if (cm.equals(methodName)) { isValid = true; break; }
                    }
                } else if (dictionary.classElements.containsKey(rawType) || dictionary.typeMembers.containsKey(rawType)) {
                    Map<String, String> members = dictionary.typeMembers.get(rawType);
                    if (members != null && members.containsKey(methodName)) {
                        isValid = true;
                    }
                    if (!isValid) {
                        for (String cm : COMMON_METHODS) {
                            if (cm.equals(methodName)) { isValid = true; break; }
                        }
                    }
                } else {
                    isValid = true;
                }
                
                if (!isValid) {
                    errors.add(new TextRange(methodOffset, methodLength, "Method '" + methodName + "' is not defined in class '" + rawType + "'"));
                }
            }
        }
        return errors;
    }

    /**
     * Validates basic syntax like unmatched brackets.
     */
    public List<TextRange> validateSyntax(String text, LanguageDef currentLangDef) {
        List<TextRange> errors = new ArrayList<>();
        if (text == null || text.isEmpty()) return errors;

        java.util.Stack<Integer> openParen = new java.util.Stack<>();
        java.util.Stack<Integer> openBrace = new java.util.Stack<>();
        java.util.Stack<Integer> openBracket = new java.util.Stack<>();

        boolean inString = false;
        boolean inChar = false;
        boolean inSingleComment = false;
        boolean inMultiComment = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char next = (i + 1 < text.length()) ? text.charAt(i + 1) : '\0';
            char prev = (i > 0) ? text.charAt(i - 1) : '\0';

            if (inSingleComment) {
                if (c == '\n') inSingleComment = false;
                continue;
            }
            if (inMultiComment) {
                if (c == '*' && next == '/') {
                    inMultiComment = false;
                    i++;
                }
                continue;
            }
            if (inString) {
                if (c == '"' && prev != '\\') inString = false;
                continue;
            }
            if (inChar) {
                if (c == '\'' && prev != '\\') inChar = false;
                continue;
            }

            if (c == '/' && next == '/') {
                inSingleComment = true;
                i++;
                continue;
            }
            if (c == '/' && next == '*') {
                inMultiComment = true;
                i++;
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '\'') {
                inChar = true;
                continue;
            }

            if (c == '(') openParen.push(i);
            else if (c == '{') openBrace.push(i);
            else if (c == '[') openBracket.push(i);
            else if (c == ')') {
                if (openParen.isEmpty()) errors.add(new TextRange(i, 1, "Unmatched closing ')'"));
                else openParen.pop();
            }
            else if (c == '}') {
                if (openBrace.isEmpty()) errors.add(new TextRange(i, 1, "Unmatched closing '}'"));
                else openBrace.pop();
            }
            else if (c == ']') {
                if (openBracket.isEmpty()) errors.add(new TextRange(i, 1, "Unmatched closing ']'"));
                else openBracket.pop();
            }
        }

        while (!openParen.isEmpty()) errors.add(new TextRange(openParen.pop(), 1, "Unclosed opening '('"));
        while (!openBrace.isEmpty()) errors.add(new TextRange(openBrace.pop(), 1, "Unclosed opening '{'"));
        while (!openBracket.isEmpty()) errors.add(new TextRange(openBracket.pop(), 1, "Unclosed opening '['"));

        return errors;
    }

    private String resolveContextTypeFromText(String textBefore) {
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("std::(?:weak|shared|unique)_ptr<\\s*([A-Za-z0-9_:<>,\\s]+)\\s*>\\s+([A-Za-z0-9_]+)\\b");
        java.util.regex.Matcher m1 = p1.matcher(textBefore);
        while (m1.find()) {
            String type = m1.group(1);
            String name = m1.group(2);
            if (textBefore.endsWith(name + "->")) {
                return type;
            }
        }
        
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_:]+)\\s*\\**\\s+([A-Za-z0-9_]+)\\s*(?:=|;)");
        java.util.regex.Matcher m2 = p2.matcher(textBefore);
        while (m2.find()) {
            String type = m2.group(1);
            String name = m2.group(2);
            if (textBefore.endsWith(name + "->") && !type.equals("return") && !type.equals("new") && !type.equals("delete")) {
                return type;
            }
        }
        
        java.util.regex.Pattern p3 = java.util.regex.Pattern.compile("new\\s+([A-Za-z0-9_:]+)\\s*\\(");
        java.util.regex.Matcher m3 = p3.matcher(textBefore);
        while (m3.find()) {
            String type = m3.group(1);
            return type; // Simple fallback
        }
        
        java.util.regex.Pattern p4 = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_:]+)::([A-Za-z0-9_]+)\\(");
        java.util.regex.Matcher m4 = p4.matcher(textBefore);
        while (m4.find()) {
            String type = m4.group(1);
            return type;
        }
        
        int lastParen = textBefore.lastIndexOf('(');
        if (lastParen > 0) {
            java.util.regex.Pattern castPattern = java.util.regex.Pattern.compile("dynamic_pointer_cast<\\s*([A-Za-z0-9_]+)\\s*>");
            java.util.regex.Matcher castMatch = castPattern.matcher(textBefore);
            if (castMatch.find()) {
                return castMatch.group(1);
            }
        }
        
        return null;
    }
}
