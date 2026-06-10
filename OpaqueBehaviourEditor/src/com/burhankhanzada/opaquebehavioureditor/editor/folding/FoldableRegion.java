package com.burhankhanzada.opaquebehavioureditor.editor.folding;

/**
 * Represents a single foldable region in the editor (e.g. a brace block or multi-line comment).
 * Line numbers are 0-based and refer to the *visible* document lines.
 */
public class FoldableRegion {

    /** 0-based visible line where the region starts (the line containing opening brace or block comment) */
    private int startLine;
    /** 0-based visible line where the region ends (the line containing closing brace or block comment) */
    private int endLine;
    /** Whether this region is currently collapsed */
    private boolean collapsed;
    /** The text that was hidden when the region was collapsed */
    private String hiddenText;
    /** The document offset where the fold placeholder starts */
    private int foldOffset;
    /** The length of the placeholder text inserted when collapsed (e.g. "{...}") */
    private int placeholderLength;

    public FoldableRegion(int startLine, int endLine) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.collapsed = false;
        this.hiddenText = null;
        this.foldOffset = -1;
        this.placeholderLength = 0;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public String getHiddenText() {
        return hiddenText;
    }

    public void setHiddenText(String hiddenText) {
        this.hiddenText = hiddenText;
    }

    public int getFoldOffset() {
        return foldOffset;
    }

    public void setFoldOffset(int foldOffset) {
        this.foldOffset = foldOffset;
    }

    public int getPlaceholderLength() {
        return placeholderLength;
    }

    public void setPlaceholderLength(int placeholderLength) {
        this.placeholderLength = placeholderLength;
    }

    /**
     * Returns the number of lines this region spans (including start and end lines).
     */
    public int getLineSpan() {
        return endLine - startLine + 1;
    }

    @Override
    public String toString() {
        return "FoldableRegion[" + startLine + "-" + endLine + (collapsed ? ", COLLAPSED" : "") + "]";
    }
}
