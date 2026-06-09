package com.burhankhanzada.opaquebehavioureditor.editor;

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
        
        String lang = language == null ? "" : language.toLowerCase();

        StringBuilder result = new StringBuilder();
        int indent = 0;
        String[] lines = code.split("\\r?\\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                result.append("\n");
                continue;
            }
            
            // Clean the line of strings and comments so we only count structural braces
            String cleanLine = trimmedLine.replaceAll("\".*?\"", "").replaceAll("//.*", "");
            
            int openBraces = 0;
            int closeBraces = 0;
            for (char c : cleanLine.toCharArray()) {
                if (c == '{') openBraces++;
                if (c == '}') closeBraces++;
            }
            
            // If the line starts with a closing brace, decrease the indent immediately for this line
            if (cleanLine.startsWith("}")) {
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
