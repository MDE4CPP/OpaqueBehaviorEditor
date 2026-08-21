package de.tu_ilmenau.sse.opaquebehavioureditor.editor.text;

import java.util.Map;

import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;

/**
 * Utility class to parse expressions from a text editor and determine the context
 * or return type for auto-completion and syntax highlighting.
 */
public class ExpressionParser {

    /**
     * Parses the C++ expression preceding the caret to determine its return type.
     * e.g., `library->getBooks()->front()->` returns the type of `Book`.
     *
     * @param textBeforeCaret The raw text occurring immediately before the cursor.
     * @param dictionary      The UML dictionary used to look up member return types.
     * @return The determined C++ type (e.g., "Book"), or null if it cannot be resolved.
     */
    public static String resolveContextTypeFromText(String textBeforeCaret, ModelDictionary dictionary, String fullText) {
        // A declaration can only apply to the caret if it appears before it.
        int caret = textBeforeCaret.length();
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
                // Base variable: something the editor injects (factory, operation parameters)
                // first, then a declaration in the body itself.
                currentType = dictionary != null ? dictionary.getContextVariableType(part) : null;
                if (currentType == null) {
                    currentType = resolveVariableType(part, fullText, caret);
                }
            } else {
                // Method or property on currentType (including inherited members)
                if (currentType != null) {
                    Map<String, String> allMembers = dictionary.getAllTypeMembers(currentType);
                    if (!allMembers.isEmpty()) {
                        currentType = allMembers.get(part); // Get the return type!
                    } else {
                        currentType = null;
                    }
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
        return resolveVariableType(variableName, fullText, fullText == null ? 0 : fullText.length());
    }

    /**
     * As above, but only considers declarations that appear before {@code caret}, and takes the
     * one nearest to it - a later declaration of the same name cannot describe an earlier use.
     */
    public static String resolveVariableType(String variableName, String fullText, int caret) {
        if (variableName == null || variableName.isBlank()) return null;
        if (fullText == null) return null;

        String scope = fullText.substring(0, Math.max(0, Math.min(caret, fullText.length())));
        boolean[] isCode = TextScanState.codeMask(scope);

        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("std::(?:weak|shared|unique)_ptr<\\s*([A-Za-z0-9_:<>,\\s]+)\\s*>\\s+" + java.util.regex.Pattern.quote(variableName) + "\\b");
        String smartPtrType = lastMatch(p1.matcher(scope), isCode, null);
        if (smartPtrType != null) {
            String type = smartPtrType.trim();
            if (!type.contains("<")) { // Simple type
                return type.substring(type.lastIndexOf(':') + 1);
            }
            return type; // e.g. Bag<Author>
        }

        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b([A-Za-z0-9_:]+)\\s*\\**\\s+" + java.util.regex.Pattern.quote(variableName) + "\\b");
        String plainType = lastMatch(p2.matcher(scope), isCode, java.util.Set.of("return", "new", "delete"));
        if (plainType != null) {
            return plainType.substring(plainType.lastIndexOf(':') + 1);
        }

        return null;
    }

    /** Names that can precede an identifier without being its type. */
    private static final java.util.Set<String> NOT_A_TYPE = java.util.Set.of(
            "return", "new", "delete", "else", "case", "const", "static", "virtual", "public",
            "private", "protected", "class", "struct", "template", "typename", "operator",
            "using", "namespace", "if", "while", "for", "switch", "do", "break", "continue",
            "throw", "std");

    private static final java.util.regex.Pattern SMART_PTR_DECL = java.util.regex.Pattern.compile(
            "std::(?:weak|shared|unique)_ptr<\\s*[A-Za-z0-9_:<>,\\s]+\\s*>\\s+([A-Za-z_][A-Za-z0-9_]*)\\b");

    private static final java.util.regex.Pattern PLAIN_DECL = java.util.regex.Pattern.compile(
            "\\b([A-Za-z_][A-Za-z0-9_:<>]*)\\s*[*&]?\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*(?:=|;|\\))");

    /**
     * Names of the variables declared in {@code text} before {@code caret}. Used to offer what the
     * body itself has introduced ahead of model and library names.
     */
    public static java.util.Set<String> declaredVariables(String text, int caret) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        if (text == null) return names;

        String scope = text.substring(0, Math.max(0, Math.min(caret, text.length())));
        boolean[] isCode = TextScanState.codeMask(scope);

        java.util.regex.Matcher m = SMART_PTR_DECL.matcher(scope);
        while (m.find()) {
            if (isCode[m.start()]) names.add(m.group(1));
        }
        m = PLAIN_DECL.matcher(scope);
        while (m.find()) {
            if (!isCode[m.start()]) continue;
            if (NOT_A_TYPE.contains(m.group(1))) continue;
            names.add(m.group(2));
        }
        return names;
    }

    /** Last group-1 match that sits in real code and is not one of {@code rejected}. */
    private static String lastMatch(java.util.regex.Matcher m, boolean[] isCode, java.util.Set<String> rejected) {
        String found = null;
        while (m.find()) {
            if (m.start() < isCode.length && !isCode[m.start()]) continue;
            String type = m.group(1).trim();
            if (rejected != null && rejected.contains(type)) continue;
            found = type;
        }
        return found;
    }
}
