package com.burhankhanzada.opaquebehavioureditor.model;

/**
 * Represents a range of text within the editor, used primarily for 
 * semantic highlighting, syntax errors, and tooltip definitions.
 */
public class TextRange {
    public final int offset;
    public final int length;
    public final String message;
    
    public TextRange(int offset, int length, String message) {
        this.offset = offset;
        this.length = length;
        this.message = message;
    }
}
