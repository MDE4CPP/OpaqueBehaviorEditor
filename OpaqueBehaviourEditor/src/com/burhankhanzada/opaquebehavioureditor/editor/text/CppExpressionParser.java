package com.burhankhanzada.opaquebehavioureditor.editor.text;

import java.util.Map;

import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;

/**
 * Utility class to parse C++ expressions from a text editor and determine the context
 * or return type for auto-completion and syntax highlighting.
 */
public class CppExpressionParser {

    /**
     * Parses the C++ expression preceding the caret to determine its return type.
     * e.g., `library->getBooks()->front()->` returns the type of `Book`.
     *
     * @param textBeforeCaret The raw text occurring immediately before the cursor.
     * @param dictionary      The UML dictionary used to look up member return types.
     * @return The determined C++ type (e.g., "Book"), or null if it cannot be resolved.
     */
    public static String resolveContextTypeFromText(String textBeforeCaret, ModelDictionary dictionary, String fullText) {
        if (textBeforeCaret.endsWith("->")) {
            textBeforeCaret = textBeforeCaret.substring(0, textBeforeCaret.length() - 2);
        } else if (textBeforeCaret.endsWith(".")) {
            textBeforeCaret = textBeforeCaret.substring(0, textBeforeCaret.length() - 1);
        } else {
            return null;
        }
        
        textBeforeCaret = textBeforeCaret.stripTrailing();
        
        // Find the start of the expression.
        StringBuilder exp = new StringBuilder();
        int parens = 0;
        for (int i = textBeforeCaret.length() - 1; i >= 0; i--) {
            char c = textBeforeCaret.charAt(i);
            if (c == ')') parens++;
            else if (c == '(') parens--;
            else if (parens == 0 && !Character.isJavaIdentifierPart(c) && c != '-' && c != '>' && c != '.') {
                break;
            }
            exp.insert(0, c);
        }
        
        String expression = exp.toString();
        String[] parts = expression.split("->|\\.");
        if (parts.length == 0) return null;
        
        String currentType = null;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.endsWith("()")) {
                part = part.substring(0, part.length() - 2);
            }
            
            if (i == 0) {
                // Base variable
                currentType = resolveVariableType(part, fullText);
            } else {
                // Method or property on currentType
                if (currentType != null && dictionary.typeMembers.containsKey(currentType)) {
                    Map<String, String> members = dictionary.typeMembers.get(currentType);
                    currentType = members.get(part); // Get the return type!
                } else {
                    currentType = null;
                }
            }
            
            // Unwrap std::shared_ptr if present
            if (currentType != null && currentType.startsWith("std::shared_ptr<")) {
                currentType = currentType.substring(16, currentType.length() - 1);
            }
        }
        
        return currentType;
    }

    /**
     * Resolves the type of a variable by searching backwards through the document text
     * for its declaration (e.g., `std::shared_ptr<Book> myBook`).
     *
     * @param variableName The name of the variable to find.
     * @param fullText     The entire source code text.
     * @return The simple type name (e.g., "Book"), or null if not found.
     */
    public static String resolveVariableType(String variableName, String fullText) {
        if (variableName == null || variableName.isBlank()) return null;
        
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("std::(?:weak|shared|unique)_ptr<\\s*([A-Za-z0-9_:<>,\\s]+)\\s*>\\s+" + java.util.regex.Pattern.quote(variableName) + "\\b");
        java.util.regex.Matcher m1 = p1.matcher(fullText);
        if (m1.find()) {
            String type = m1.group(1).trim();
            if (!type.contains("<")) { // Simple type
                return type.substring(type.lastIndexOf(':') + 1);
            }
            return type; // e.g. Bag<Author>
        }
        
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_:]+)\\s*\\**\\s+" + java.util.regex.Pattern.quote(variableName) + "\\b");
        java.util.regex.Matcher m2 = p2.matcher(fullText);
        while (m2.find()) {
            String type = m2.group(1);
            if (!type.equals("return") && !type.equals("new") && !type.equals("delete")) {
                return type.substring(type.lastIndexOf(':') + 1);
            }
        }
        
        return null;
    }
}
