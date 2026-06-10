package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.ExpressionParser;

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

    public ModelDictionary getDictionary() {
        return dictionary;
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
            String rawType = ExpressionParser.resolveContextTypeFromText(textBefore, dictionary, text);
            
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


}
