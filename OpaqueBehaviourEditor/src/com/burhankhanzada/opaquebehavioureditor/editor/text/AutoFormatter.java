package com.burhankhanzada.opaquebehavioureditor.editor.text;

/**
 * Utility class to format and properly indent code snippets.
 * Provides a "best-effort" naive formatter for C-like languages 
 * (C++, Java, C) based on bracket counting.
 */
public class AutoFormatter {

    /**
     * Formats the given code snippet based on the specified language.
     * 
     * @param code     The raw code text.
     * @param language The language of the code (e.g. "CPP", "Java", "C").
     * @return The properly indented code string.
     */
    public static String format(String code, String language) {
        if (code == null || code.isBlank()) return "";

        StringBuilder result = new StringBuilder();
        int indent = 0;
        String[] lines = code.split("\\r?\\n");
        TextScanState state = new TextScanState();
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                result.append("\n");
                continue;
            }
            
            int openBraces = 0;
            int closeBraces = 0;
            boolean firstCharIsCloseBrace = false;
            boolean foundFirstChar = false;

            for (int i = 0; i < trimmedLine.length(); i++) {
                boolean wasIgnored = state.isIgnored();
                int skip = state.process(trimmedLine, i);
                
                if (!wasIgnored && !state.isIgnored()) {
                    char c = trimmedLine.charAt(i);
                    if (!foundFirstChar && !Character.isWhitespace(c)) {
                        foundFirstChar = true;
                        if (c == '}') firstCharIsCloseBrace = true;
                    }
                    if (c == '{') openBraces++;
                    if (c == '}') closeBraces++;
                }
                i += skip;
            }
            state.inSingleComment = false; // Reset single comment at line end
            
            // If the line starts with a closing brace, decrease the indent immediately for this line
            if (firstCharIsCloseBrace) {
                indent = Math.max(0, indent - 1);
                closeBraces--; // Don't count this brace again for the next line's calculation
            }
            
            // Append the correct indentation
            for (int i = 0; i < indent; i++) {
                result.append("    ");
            }
            
            result.append(trimmedLine).append("\n");
            
            // Calculate the indentation for the NEXT line
            indent += openBraces;
            indent = Math.max(0, indent - closeBraces);
        }
        
        // Remove trailing newline
        return result.toString().replaceFirst("\\s+$", "");
    }
}
