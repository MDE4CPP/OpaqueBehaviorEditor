package com.burhankhanzada.opaquebehavioureditor.editor.folding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;

/**
 * Manages foldable regions for the code editor. Detects brace blocks ({...})
 * and multi-line comments, and handles collapsing/expanding them
 * by manipulating the StyledText content directly.
 */
public class FoldingManager {

    private final StyledText styledText;
    private final List<FoldableRegion> regions = new ArrayList<>();
    
    /** Listeners notified when folding state changes */
    private final List<Runnable> foldingListeners = new ArrayList<>();

    public FoldingManager(StyledText styledText) {
        this.styledText = styledText;
    }

    /**
     * Returns an unmodifiable view of the current foldable regions.
     */
    public List<FoldableRegion> getRegions() {
        return Collections.unmodifiableList(regions);
    }

    /**
     * Add a listener that is notified whenever fold state changes.
     */
    public void addFoldingListener(Runnable listener) {
        foldingListeners.add(listener);
    }

    /**
     * Recomputes all foldable regions from the current document text.
     * Should be called after text modifications. Preserves collapsed state
     * for regions that still exist at the same start content.
     */
    public void recomputeRegions() {
        if (styledText == null || styledText.isDisposed()) return;

        // Save collapsed state keyed by the content of the start line
        List<String> previouslyCollapsedStartLines = new ArrayList<>();
        for (FoldableRegion r : regions) {
            if (r.isCollapsed()) {
                int line = r.getStartLine();
                if (line < styledText.getLineCount()) {
                    previouslyCollapsedStartLines.add(styledText.getLine(line).trim());
                }
            }
        }

        regions.clear();
        String text = styledText.getText();
        detectBraceRegions(text);
        detectCommentRegions(text);

        // Sort by start line
        regions.sort((a, b) -> Integer.compare(a.getStartLine(), b.getStartLine()));
    }

    /**
     * Detects matching { } pairs that span multiple lines.
     * Ignores braces inside string literals and line/block comments.
     */
    private void detectBraceRegions(String text) {
        Stack<Integer> braceStack = new Stack<>();

        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char stringDelimiter = 0;

        int currentLine = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char prev = i > 0 ? text.charAt(i - 1) : 0;
            char next = i < text.length() - 1 ? text.charAt(i + 1) : 0;

            // Track line boundaries
            if (c == '\n') {
                currentLine++;
                inLineComment = false;
                continue;
            }
            if (c == '\r') {
                if (next == '\n') {
                    continue; // handled by \n
                }
                currentLine++;
                inLineComment = false;
                continue;
            }

            // Skip escaped characters
            if (prev == '\\' && (inString || inChar)) {
                continue;
            }

            // String/char literal tracking
            if (!inLineComment && !inBlockComment) {
                if (c == '"' && !inChar) {
                    if (inString && stringDelimiter == '"') {
                        inString = false;
                    } else if (!inString) {
                        inString = true;
                        stringDelimiter = '"';
                    }
                    continue;
                }
                if (c == '\'' && !inString) {
                    if (inChar) {
                        inChar = false;
                    } else {
                        inChar = true;
                    }
                    continue;
                }
            }

            if (inString || inChar) continue;

            // Comment tracking
            if (!inBlockComment && c == '/' && next == '/') {
                inLineComment = true;
                continue;
            }
            if (!inLineComment && c == '/' && next == '*') {
                inBlockComment = true;
                i++; // skip the '*'
                continue;
            }
            if (inBlockComment && c == '*' && next == '/') {
                inBlockComment = false;
                i++; // skip the '/'
                continue;
            }

            if (inLineComment || inBlockComment) continue;

            // Brace matching
            if (c == '{') {
                braceStack.push(currentLine);
            } else if (c == '}' && !braceStack.isEmpty()) {
                int startLine = braceStack.pop();
                int endLine = currentLine;
                // Only fold if it spans at least 2 lines
                if (endLine > startLine) {
                    regions.add(new FoldableRegion(startLine, endLine));
                }
            }
        }
    }

    /**
     * Detects multi-line block comments that span 2+ lines.
     */
    private void detectCommentRegions(String text) {
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        int blockCommentStartLine = -1;
        int currentLine = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            char prev = i > 0 ? text.charAt(i - 1) : 0;
            char next = i < text.length() - 1 ? text.charAt(i + 1) : 0;

            if (c == '\n') {
                currentLine++;
                inLineComment = false;
                continue;
            }
            if (c == '\r') {
                if (next == '\n') continue;
                currentLine++;
                inLineComment = false;
                continue;
            }

            if (prev == '\\' && (inString || inChar)) continue;

            if (!inLineComment && blockCommentStartLine < 0) {
                if (c == '"' && !inChar) {
                    inString = !inString;
                    continue;
                }
                if (c == '\'' && !inString) {
                    inChar = !inChar;
                    continue;
                }
            }

            if (inString || inChar) continue;

            if (blockCommentStartLine < 0 && c == '/' && next == '/') {
                inLineComment = true;
                continue;
            }

            if (inLineComment) continue;

            if (blockCommentStartLine < 0 && c == '/' && next == '*') {
                blockCommentStartLine = currentLine;
                i++; // skip '*'
                continue;
            }
            if (blockCommentStartLine >= 0 && c == '*' && next == '/') {
                int endLine = currentLine;
                if (endLine > blockCommentStartLine) {
                    regions.add(new FoldableRegion(blockCommentStartLine, endLine));
                }
                blockCommentStartLine = -1;
                i++; // skip '/'
                continue;
            }
        }
    }

    /**
     * Toggles the fold state of the region that starts at the given visible line.
     * @param visibleLine 0-based visible line number
     */
    public void toggleFold(int visibleLine) {
        for (FoldableRegion region : regions) {
            if (region.getStartLine() == visibleLine) {
                if (region.isCollapsed()) {
                    expand(region);
                } else {
                    collapse(region);
                }
                fireFoldingChanged();
                return;
            }
        }
    }

    /**
     * Collapses a region: hides lines startLine+1 through endLine,
     * replacing them with a placeholder.
     */
    private void collapse(FoldableRegion region) {
        if (region.isCollapsed()) return;
        
        int startLine = region.getStartLine();
        int endLine = region.getEndLine();
        int lineCount = styledText.getLineCount();

        if (startLine >= lineCount || endLine >= lineCount) return;

        try {
            // Get the offset at the end of the start line (before newline)
            int startLineOffset = styledText.getOffsetAtLine(startLine);
            String startLineContent = styledText.getLine(startLine);
            int foldStart = startLineOffset + startLineContent.length();

            // Get the offset at the end of the end line
            int endLineOffset = styledText.getOffsetAtLine(endLine);
            String endLineContent = styledText.getLine(endLine);
            int foldEnd = endLineOffset + endLineContent.length();

            // The text to hide: everything from end-of-startLine to end-of-endLine
            String textToHide = styledText.getText(foldStart, foldEnd - 1);

            // Replace with placeholder
            String placeholder = " ··· ";

            region.setHiddenText(textToHide);
            region.setFoldOffset(foldStart);
            region.setPlaceholderLength(placeholder.length());
            region.setCollapsed(true);

            styledText.replaceTextRange(foldStart, textToHide.length(), placeholder);

            // Clear any error underline styles on the entire folded line + placeholder
            clearUnderlinesOnRange(startLineOffset, startLineContent.length() + placeholder.length());

            // Also schedule an async clear to catch deferred style applications
            final int clearStart = startLineOffset;
            final int clearLen = startLineContent.length() + placeholder.length();
            styledText.getDisplay().asyncExec(() -> {
                if (!styledText.isDisposed()) {
                    clearUnderlinesOnRange(clearStart, clearLen);
                }
            });

            // Update all other regions' line numbers
            int linesRemoved = endLine - startLine;
            adjustRegionsAfterFold(region, -linesRemoved);

        } catch (Exception e) {
            // Safety: if anything goes wrong, don't corrupt state
            region.setCollapsed(false);
            region.setHiddenText(null);
            e.printStackTrace();
        }
    }

    /**
     * Expands a previously collapsed region, restoring the hidden text.
     */
    private void expand(FoldableRegion region) {
        if (!region.isCollapsed() || region.getHiddenText() == null) return;

        try {
            int foldOffset = region.getFoldOffset();
            int placeholderLen = region.getPlaceholderLength();
            String hiddenText = region.getHiddenText();

            styledText.replaceTextRange(foldOffset, placeholderLen, hiddenText);

            int linesRestored = region.getEndLine() - region.getStartLine();
            region.setCollapsed(false);
            region.setHiddenText(null);
            region.setFoldOffset(-1);
            region.setPlaceholderLength(0);

            // Restore line numbers for downstream regions
            adjustRegionsAfterFold(region, linesRestored);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * After collapsing or expanding a region, adjust the line numbers of all
     * regions that come after it.
     * @param changedRegion the region that was just folded/unfolded
     * @param lineDelta negative for collapse, positive for expand
     */
    private void adjustRegionsAfterFold(FoldableRegion changedRegion, int lineDelta) {
        int thresholdLine = changedRegion.getStartLine();
        for (FoldableRegion r : regions) {
            if (r == changedRegion) continue;
            if (r.getStartLine() > thresholdLine) {
                r.setStartLine(r.getStartLine() + lineDelta);
                r.setEndLine(r.getEndLine() + lineDelta);
            }
        }
    }

    /**
     * Collapses the region containing the given visible line (if any).
     */
    public void collapseAtLine(int visibleLine) {
        for (FoldableRegion region : regions) {
            if (region.getStartLine() == visibleLine && !region.isCollapsed()) {
                collapse(region);
                fireFoldingChanged();
                return;
            }
        }
    }

    /**
     * Expands the region containing the given visible line (if any).
     */
    public void expandAtLine(int visibleLine) {
        for (FoldableRegion region : regions) {
            if (region.getStartLine() == visibleLine && region.isCollapsed()) {
                expand(region);
                fireFoldingChanged();
                return;
            }
        }
    }

    /**
     * Expands all collapsed regions (from bottom to top to keep offsets valid).
     */
    public void expandAll() {
        // Expand in reverse order to keep offsets stable
        List<FoldableRegion> collapsedRegions = new ArrayList<>();
        for (FoldableRegion r : regions) {
            if (r.isCollapsed()) collapsedRegions.add(r);
        }
        // Sort by startLine descending
        collapsedRegions.sort((a, b) -> Integer.compare(b.getStartLine(), a.getStartLine()));
        for (FoldableRegion r : collapsedRegions) {
            expand(r);
        }
        fireFoldingChanged();
    }

    /**
     * Returns the FoldableRegion whose start line matches the given visible line, or null.
     */
    public FoldableRegion getRegionAtLine(int visibleLine) {
        for (FoldableRegion r : regions) {
            if (r.getStartLine() == visibleLine) return r;
        }
        return null;
    }

    /**
     * Returns true if there are any collapsed regions.
     */
    public boolean hasCollapsedRegions() {
        for (FoldableRegion r : regions) {
            if (r.isCollapsed()) return true;
        }
        return false;
    }

    /**
     * Reconstructs the full unfolded text by reinserting hidden text
     * from all collapsed regions into the current visible text.
     * This allows validators to work on the complete document
     * even when regions are folded.
     */
    public String getUnfoldedText() {
        if (styledText == null || styledText.isDisposed()) return "";
        String text = styledText.getText();
        if (!hasCollapsedRegions()) return text;

        // Collect collapsed regions sorted by foldOffset descending
        // so we can replace from end to start without invalidating offsets
        List<FoldableRegion> collapsed = new ArrayList<>();
        for (FoldableRegion r : regions) {
            if (r.isCollapsed() && r.getFoldOffset() >= 0 && r.getHiddenText() != null) {
                collapsed.add(r);
            }
        }
        collapsed.sort((a, b) -> Integer.compare(b.getFoldOffset(), a.getFoldOffset()));

        StringBuilder sb = new StringBuilder(text);
        for (FoldableRegion r : collapsed) {
            int offset = r.getFoldOffset();
            int placeholderLen = r.getPlaceholderLength();
            if (offset >= 0 && offset + placeholderLen <= sb.length()) {
                sb.replace(offset, offset + placeholderLen, r.getHiddenText());
            }
        }
        return sb.toString();
    }

    private void fireFoldingChanged() {
        for (Runnable listener : foldingListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Clears any underline styles on the given text range,
     * preserving all other style properties (colors, fonts, etc.).
     */
    private void clearUnderlinesOnRange(int start, int length) {
        try {
            int end = Math.min(start + length, styledText.getCharCount());
            if (start >= end || start < 0) return;

            StyleRange[] ranges = styledText.getStyleRanges(start, end - start);
            for (StyleRange sr : ranges) {
                if (sr.underline) {
                    sr.underline = false;
                    sr.underlineStyle = 0;
                    sr.underlineColor = null;
                    styledText.setStyleRange(sr);
                }
            }
        } catch (Exception e) {
            // Ignore — defensive against race conditions during text changes
        }
    }
}
