package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.burhankhanzada.opaquebehavioureditor.editor.folding.FoldingManager;

/**
 * Keyboard-triggered fold/unfold actions.
 * Supports collapsing and expanding the region at the current cursor line.
 */
public class FoldingAction {

    private final SourceViewer sourceViewer;
    private final FoldingManager foldingManager;

    public FoldingAction(SourceViewer sourceViewer, FoldingManager foldingManager) {
        this.sourceViewer = sourceViewer;
        this.foldingManager = foldingManager;
    }

    /**
     * Collapses the foldable region at the current caret line.
     */
    public void collapseCurrentRegion() {
        int line = getCurrentLine();
        if (line >= 0) {
            foldingManager.collapseAtLine(line);
            redraw();
        }
    }

    /**
     * Expands the foldable region at the current caret line.
     */
    public void expandCurrentRegion() {
        int line = getCurrentLine();
        if (line >= 0) {
            foldingManager.expandAtLine(line);
            redraw();
        }
    }

    /**
     * Toggles the fold state of the region at the current caret line.
     */
    public void toggleCurrentRegion() {
        int line = getCurrentLine();
        if (line >= 0) {
            foldingManager.toggleFold(line);
            redraw();
        }
    }

    /**
     * Expands all collapsed regions.
     */
    public void expandAll() {
        foldingManager.expandAll();
        redraw();
    }

    private int getCurrentLine() {
        StyledText st = sourceViewer.getTextWidget();
        if (st == null || st.isDisposed()) return -1;
        int offset = st.getCaretOffset();
        return st.getLineAtOffset(offset);
    }

    private void redraw() {
        StyledText st = sourceViewer.getTextWidget();
        if (st != null && !st.isDisposed()) {
            st.redraw();
        }
    }
}
