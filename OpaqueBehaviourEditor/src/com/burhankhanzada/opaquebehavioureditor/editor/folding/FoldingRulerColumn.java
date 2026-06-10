package com.burhankhanzada.opaquebehavioureditor.editor.folding;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;

/**
 * A PaintListener that draws fold/expand icons (▶/▼ triangles) in the left
 * margin of the StyledText widget, immediately after the line number separator.
 * Also handles mouse clicks on the fold icons to toggle fold state.
 */
public class FoldingRulerColumn implements PaintListener {

    /** X position where fold icons start (right after the line number separator) */
    private static final int FOLD_ICON_X = 46;
    /** Size of the fold triangle icon */
    private static final int ICON_SIZE = 8;
    /** Clickable area width for fold icons */
    private static final int CLICK_AREA_WIDTH = 14;

    private final StyledText styledText;
    private final FoldingManager foldingManager;
    private final Color iconColor;
    private final Color hoverColor;

    /** The line currently hovered over (for highlighting), -1 if none */
    private int hoveredLine = -1;

    public FoldingRulerColumn(StyledText styledText, FoldingManager foldingManager,
                              Color iconColor, Color hoverColor) {
        this.styledText = styledText;
        this.foldingManager = foldingManager;
        this.iconColor = iconColor;
        this.hoverColor = hoverColor;

        setupMouseHandlers();
    }

    @Override
    public void paintControl(PaintEvent e) {
        if (foldingManager == null) return;

        int topIndex = styledText.getTopIndex();
        int lineHeight = styledText.getLineHeight();
        int visibleLines = (styledText.getClientArea().height + lineHeight - 1) / lineHeight;
        int bottomIndex = Math.min(topIndex + visibleLines, styledText.getLineCount() - 1);

        for (int i = topIndex; i <= bottomIndex; i++) {
            FoldableRegion region = foldingManager.getRegionAtLine(i);
            if (region != null) {
                int linePixel = styledText.getLinePixel(i);
                boolean isHovered = (i == hoveredLine);
                
                if (region.isCollapsed()) {
                    drawCollapsedIcon(e.gc, linePixel, lineHeight, isHovered);
                } else {
                    drawExpandedIcon(e.gc, linePixel, lineHeight, isHovered);
                }
            }
        }
    }

    /**
     * Draws a right-pointing triangle ▶ for collapsed regions.
     */
    private void drawCollapsedIcon(GC gc, int linePixelY, int lineHeight, boolean hovered) {
        gc.setBackground(hovered ? hoverColor : iconColor);
        gc.setForeground(hovered ? hoverColor : iconColor);

        int centerY = linePixelY + (lineHeight / 2);
        int x = FOLD_ICON_X;

        // Right-pointing triangle: ▶
        int[] triangle = new int[] {
            x, centerY - ICON_SIZE / 2,          // top-left
            x + ICON_SIZE, centerY,                // right-center
            x, centerY + ICON_SIZE / 2             // bottom-left
        };
        gc.fillPolygon(triangle);
    }

    /**
     * Draws a down-pointing triangle ▼ for expanded (unfoldable) regions.
     */
    private void drawExpandedIcon(GC gc, int linePixelY, int lineHeight, boolean hovered) {
        gc.setBackground(hovered ? hoverColor : iconColor);
        gc.setForeground(hovered ? hoverColor : iconColor);

        int centerY = linePixelY + (lineHeight / 2);
        int x = FOLD_ICON_X;

        // Down-pointing triangle: ▼
        int[] triangle = new int[] {
            x, centerY - ICON_SIZE / 2 + 1,                     // top-left
            x + ICON_SIZE, centerY - ICON_SIZE / 2 + 1,         // top-right
            x + ICON_SIZE / 2, centerY + ICON_SIZE / 2 - 1      // bottom-center
        };
        gc.fillPolygon(triangle);
    }

    /**
     * Sets up mouse listeners for click-to-fold and hover highlighting.
     */
    private void setupMouseHandlers() {
        styledText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                // Only respond to clicks in the fold icon area (left margin)
                if (e.x >= FOLD_ICON_X && e.x <= FOLD_ICON_X + CLICK_AREA_WIDTH) {
                    try {
                        int lineIndex = getLineAtY(e.y);
                        if (lineIndex >= 0) {
                            FoldableRegion region = foldingManager.getRegionAtLine(lineIndex);
                            if (region != null) {
                                foldingManager.toggleFold(lineIndex);
                                styledText.redraw();
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore click calculation errors
                    }
                }
            }
        });

        styledText.addMouseMoveListener(e -> {
            int newHovered = -1;
            if (e.x >= FOLD_ICON_X && e.x <= FOLD_ICON_X + CLICK_AREA_WIDTH) {
                try {
                    int lineIndex = getLineAtY(e.y);
                    if (lineIndex >= 0 && foldingManager.getRegionAtLine(lineIndex) != null) {
                        newHovered = lineIndex;
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            }
            if (newHovered != hoveredLine) {
                hoveredLine = newHovered;
                // Only redraw the fold icon area
                styledText.redraw(FOLD_ICON_X, 0, CLICK_AREA_WIDTH, styledText.getClientArea().height, false);
            }
        });
    }

    /**
     * Converts a pixel Y coordinate to a line index.
     */
    private int getLineAtY(int y) {
        int topIndex = styledText.getTopIndex();
        int lineHeight = styledText.getLineHeight();
        int lineCount = styledText.getLineCount();

        for (int i = topIndex; i < lineCount; i++) {
            int linePixel = styledText.getLinePixel(i);
            if (y >= linePixel && y < linePixel + lineHeight) {
                return i;
            }
        }
        return -1;
    }
}
