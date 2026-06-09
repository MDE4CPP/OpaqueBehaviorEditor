package com.burhankhanzada.opaquebehavioureditor.editor.text;

public class TextUtilities {
    
    private TextUtilities() {
        // Utility class
    }
    
    /**
     * Extracts the Java identifier surrounding the given offset in the text.
     * Starts at offset and expands backwards and forwards.
     * 
     * @param text The full document text
     * @param offset The cursor offset
     * @return The extracted word, or an empty string if none found.
     */
    public static String getWordAtOffset(String text, int offset) {
        int[] bounds = getWordBounds(text, offset);
        if (bounds[0] < bounds[1]) {
            return text.substring(bounds[0], bounds[1]);
        }
        return "";
    }
    
    /**
     * Gets the [start, end] indices of the Java identifier surrounding the given offset.
     * 
     * @param text The full document text
     * @param offset The cursor offset
     * @return an int array where arr[0] is start offset and arr[1] is end offset.
     */
    public static int[] getWordBounds(String text, int offset) {
        if (text == null || offset < 0 || offset > text.length()) {
            return new int[] { offset, offset };
        }
        
        int start = offset;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        
        int end = offset;
        while (end < text.length() && Character.isJavaIdentifierPart(text.charAt(end))) {
            end++;
        }
        
        return new int[] { start, end };
    }
    
    /**
     * Extracts the text on the current line immediately before the identifier at the offset.
     * Used for resolving member access contexts (e.g. "myObject." before "myProperty").
     * 
     * @param text The full document text
     * @param offset The cursor offset
     * @return The text immediately before the current identifier, stripped of trailing whitespace.
     */
    public static String getTextBeforeIdentifier(String text, int offset) {
        int[] bounds = getWordBounds(text, offset);
        int start = bounds[0];
        if (start > 0 && start <= text.length()) {
            return text.substring(0, start).stripTrailing();
        }
        return "";
    }
}
