package com.burhankhanzada.opaquebehavioureditor.editor.text;

public class TextScanState {
    public boolean inString = false;
    public boolean inChar = false;
    public boolean inSingleComment = false;
    public boolean inMultiComment = false;

    public boolean isIgnored() {
        return inString || inChar || inSingleComment || inMultiComment;
    }

    /**
     * Processes the character at index i and returns the number of extra characters to skip.
     * (e.g. returns 1 for 2-character tokens like single-line and multi-line comment markers).
     */
    public int process(String text, int i) {
        char c = text.charAt(i);
        char next = (i + 1 < text.length()) ? text.charAt(i + 1) : '\0';
        char prev = (i > 0) ? text.charAt(i - 1) : '\0';

        if (inSingleComment) {
            if (c == '\n' || c == '\r') inSingleComment = false;
            return 0;
        }
        if (inMultiComment) {
            if (c == '*' && next == '/') {
                inMultiComment = false;
                return 1;
            }
            return 0;
        }
        if (inString) {
            if (c == '"' && prev != '\\') inString = false;
            return 0;
        }
        if (inChar) {
            if (c == '\'' && prev != '\\') inChar = false;
            return 0;
        }

        if (c == '/' && next == '/') {
            inSingleComment = true;
            return 1;
        }
        if (c == '/' && next == '*') {
            inMultiComment = true;
            return 1;
        }
        if (c == '"') {
            inString = true;
            return 0;
        }
        if (c == '\'') {
            inChar = true;
            return 0;
        }

        return 0;
    }
}
