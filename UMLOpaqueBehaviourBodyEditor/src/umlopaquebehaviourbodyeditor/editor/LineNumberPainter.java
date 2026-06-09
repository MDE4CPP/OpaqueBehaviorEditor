package umlopaquebehaviourbodyeditor.editor;

import umlopaquebehaviourbodyeditor.ui.*;
import umlopaquebehaviourbodyeditor.editor.*;
import umlopaquebehaviourbodyeditor.model.*;


import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;

/**
 * A custom PaintListener that draws line numbers and a separator line
 * in the left margin of a StyledText widget.
 */
public class LineNumberPainter implements PaintListener {
    private final StyledText codeText;
    private final Color lineNumColor;
    private final Color separatorColor;

    public LineNumberPainter(StyledText codeText, Color lineNumColor, Color separatorColor) {
        this.codeText = codeText;
        this.lineNumColor = lineNumColor;
        this.separatorColor = separatorColor;
    }

    @Override
    public void paintControl(PaintEvent e) {
        int topIndex = codeText.getTopIndex();
        int lineHeight = codeText.getLineHeight();
        int visibleLines = (codeText.getClientArea().height + lineHeight - 1) / lineHeight;
        int bottomIndex = Math.min(topIndex + visibleLines, codeText.getLineCount() - 1);
        
        e.gc.setForeground(lineNumColor);
        
        for (int i = topIndex; i <= bottomIndex; i++) {
            int linePixel = codeText.getLinePixel(i);
            String num = String.valueOf(i + 1);
            Point extent = e.gc.stringExtent(num);
            // Right align within the left 45px margin area
            e.gc.drawString(num, 38 - extent.x, linePixel, true);
        }
        
        // Draw a separator line between numbers and text
        e.gc.setForeground(separatorColor);
        e.gc.drawLine(42, 0, 42, codeText.getClientArea().height);
    }
}
