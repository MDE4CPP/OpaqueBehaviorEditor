package com.burhankhanzada.opaquebehavioureditor.editor.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class CompletionPopup {

    private final StyledText styledText;
    private Shell popupShell;
    private Table proposalTable;
    private final boolean darkTheme;
    private Runnable onAccept;

    public CompletionPopup(StyledText styledText) {
        this.styledText = styledText;
        this.darkTheme = isDarkTheme(styledText.getDisplay());
    }

    public void setOnAccept(Runnable onAccept) {
        this.onAccept = onAccept;
    }

    public void show(List<String> proposals) {
        if (popupShell == null || popupShell.isDisposed()) {
            createPopup();
        }

        proposalTable.removeAll();
        for (String p : proposals) {
            TableItem item = new TableItem(proposalTable, SWT.NONE);
            item.setText(p);
        }

        // Position below the current word
        Point caretLocation = styledText.getCaret().getLocation();
        Point displayPoint = styledText.toDisplay(caretLocation.x, caretLocation.y + styledText.getLineHeight());
        popupShell.setLocation(displayPoint);
        popupShell.setSize(280, Math.min(proposals.size() * 22 + 6, CompletionEngine.MAX_PROPOSALS * 22 + 6));

        if (!popupShell.isVisible()) {
            popupShell.setVisible(true);
        }

        // Select first item
        if (proposalTable.getItemCount() > 0) {
            proposalTable.select(0);
        }
    }

    private void createPopup() {
        popupShell = new Shell(styledText.getShell(), SWT.NO_TRIM | SWT.ON_TOP);
        popupShell.setLayout(new FillLayout());

        proposalTable = new Table(popupShell, SWT.SINGLE | SWT.FULL_SELECTION);
        proposalTable.setFont(styledText.getFont());

        if (darkTheme) {
            proposalTable.setBackground(new Color(new RGB(37, 37, 38)));
            proposalTable.setForeground(new Color(new RGB(212, 212, 212)));
        }

        // Double-click to accept
        proposalTable.addListener(SWT.DefaultSelection, e -> fireAccept());

        // Single-click to accept
        proposalTable.addListener(SWT.Selection, e -> {
            // Delay so the selection registers first
            styledText.getDisplay().timerExec(50, this::fireAccept);
        });
    }

    public void dismiss() {
        if (popupShell != null && !popupShell.isDisposed()) {
            popupShell.setVisible(false);
        }
    }

    public boolean isVisible() {
        return popupShell != null && !popupShell.isDisposed() && popupShell.isVisible();
    }

    public void navigate(int direction) {
        if (!isVisible()) return;
        int count = proposalTable.getItemCount();
        if (count == 0) return;
        int current = proposalTable.getSelectionIndex();
        int next = Math.max(0, Math.min(count - 1, current + direction));
        proposalTable.select(next);
    }

    public String getSelectedProposal() {
        if (!isVisible()) return null;
        int idx = proposalTable.getSelectionIndex();
        if (idx < 0) return null;
        return proposalTable.getItem(idx).getText();
    }

    private void fireAccept() {
        if (onAccept != null) onAccept.run();
    }

    public boolean isFocusControl() {
        return popupShell != null && !popupShell.isDisposed() && popupShell.isFocusControl();
    }

    private static boolean isDarkTheme(Display display) {
        Color bg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        double brightness = (bg.getRed() * 299.0 + bg.getGreen() * 587.0 + bg.getBlue() * 114.0) / 1000.0;
        return brightness < 128;
    }
}
