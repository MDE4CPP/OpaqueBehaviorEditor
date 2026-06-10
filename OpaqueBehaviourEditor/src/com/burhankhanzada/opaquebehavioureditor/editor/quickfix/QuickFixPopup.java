package com.burhankhanzada.opaquebehavioureditor.editor.quickfix;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * A popup that shows quick-fix suggestions for validation errors.
 * Displays the error message and a list of "Did you mean ...?" suggestions.
 * Clicking a suggestion applies the fix by replacing the erroneous text.
 */
public class QuickFixPopup {

    private final StyledText styledText;
    private Shell popupShell;
    private Table suggestionTable;
    private Label errorLabel;
    private final boolean darkTheme;

    /** The offset and length of the erroneous text in the document */
    private int errorOffset;
    private int errorLength;

    public QuickFixPopup(StyledText styledText) {
        this.styledText = styledText;
        this.darkTheme = isDarkTheme(styledText.getDisplay());
    }

    /**
     * Shows the quick-fix popup at the given error location.
     * @param errorMessage The validation error message
     * @param suggestions  The list of suggestions from QuickFixEngine
     * @param offset       The document offset of the erroneous method name
     * @param length       The length of the erroneous method name
     */
    public void show(String errorMessage, List<QuickFixEngine.Suggestion> suggestions, int offset, int length) {
        if (suggestions == null || suggestions.isEmpty()) return;

        this.errorOffset = offset;
        this.errorLength = length;

        if (popupShell == null || popupShell.isDisposed()) {
            createPopup();
        }

        // Set error message
        errorLabel.setText("\u26A0 " + errorMessage);

        // Populate suggestions
        suggestionTable.removeAll();
        for (QuickFixEngine.Suggestion s : suggestions) {
            TableItem item = new TableItem(suggestionTable, SWT.NONE);
            item.setText("\u2192 Did you mean: " + s.getDisplayLabel());
            item.setData(s);
        }

        // Position below the error location
        try {
            Point caretLocation = styledText.getLocationAtOffset(offset);
            Point displayPoint = styledText.toDisplay(caretLocation.x, caretLocation.y + styledText.getLineHeight());
            popupShell.setLocation(displayPoint.x, displayPoint.y + 2);
        } catch (Exception e) {
            // Fallback: position at caret
            Point caretLoc = styledText.getCaret().getLocation();
            Point displayPt = styledText.toDisplay(caretLoc.x, caretLoc.y + styledText.getLineHeight());
            popupShell.setLocation(displayPt);
        }

        // Calculate popup size
        GC gc = new GC(popupShell);
        int maxWidth = gc.stringExtent(errorLabel.getText()).x + 40;
        for (int i = 0; i < suggestionTable.getItemCount(); i++) {
            int w = gc.stringExtent(suggestionTable.getItem(i).getText()).x + 40;
            maxWidth = Math.max(maxWidth, w);
        }
        gc.dispose();
        maxWidth = Math.max(maxWidth, 300);

        int tableHeight = Math.min(suggestions.size() * 22 + 4, 5 * 22 + 4);
        popupShell.setSize(maxWidth, tableHeight + 30);

        popupShell.setVisible(true);

        // Select first suggestion
        if (suggestionTable.getItemCount() > 0) {
            suggestionTable.select(0);
        }

        // Dismiss when the user clicks elsewhere
        styledText.getDisplay().addFilter(SWT.MouseDown, e -> {
            if (popupShell != null && !popupShell.isDisposed() && popupShell.isVisible()) {
                if (e.widget != suggestionTable && e.widget != errorLabel) {
                    dismiss();
                }
            }
        });
    }

    private void createPopup() {
        popupShell = new Shell(styledText.getShell(), SWT.NO_TRIM | SWT.ON_TOP);

        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 4;
        layout.marginHeight = 4;
        layout.verticalSpacing = 2;
        popupShell.setLayout(layout);

        Color bgColor;
        Color fgColor;
        Color errorFgColor;
        Color borderColor;

        if (darkTheme) {
            bgColor = new Color(new RGB(37, 37, 38));
            fgColor = new Color(new RGB(212, 212, 212));
            errorFgColor = new Color(new RGB(255, 140, 140));
            borderColor = new Color(new RGB(69, 69, 69));
        } else {
            bgColor = new Color(new RGB(255, 255, 255));
            fgColor = new Color(new RGB(30, 30, 30));
            errorFgColor = new Color(new RGB(180, 0, 0));
            borderColor = new Color(new RGB(200, 200, 200));
        }

        popupShell.setBackground(borderColor);

        Composite inner = new Composite(popupShell, SWT.NONE);
        inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout innerLayout = new GridLayout(1, false);
        innerLayout.marginWidth = 4;
        innerLayout.marginHeight = 2;
        innerLayout.verticalSpacing = 2;
        inner.setLayout(innerLayout);
        inner.setBackground(bgColor);

        // Error message label
        errorLabel = new Label(inner, SWT.NONE);
        errorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        errorLabel.setBackground(bgColor);
        errorLabel.setForeground(errorFgColor);

        // Make the error label font slightly smaller
        FontData[] fd = styledText.getFont().getFontData();
        if (fd.length > 0) {
            fd[0].setHeight(fd[0].getHeight() - 1);
            errorLabel.setFont(new Font(styledText.getDisplay(), fd[0]));
        }

        // Suggestion table
        suggestionTable = new Table(inner, SWT.SINGLE | SWT.FULL_SELECTION);
        suggestionTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        suggestionTable.setFont(styledText.getFont());
        suggestionTable.setBackground(bgColor);
        suggestionTable.setForeground(fgColor);
        suggestionTable.setLinesVisible(false);
        suggestionTable.setHeaderVisible(false);

        // Click/Enter to apply the suggestion
        suggestionTable.addListener(SWT.DefaultSelection, e -> applySuggestion());
        suggestionTable.addListener(SWT.Selection, e -> {
            styledText.getDisplay().timerExec(50, this::applySuggestion);
        });

        // Keyboard navigation
        suggestionTable.addListener(SWT.KeyDown, e -> {
            if (e.keyCode == SWT.ESC) {
                dismiss();
            }
        });
    }

    /**
     * Applies the currently selected suggestion by replacing the erroneous text.
     */
    private void applySuggestion() {
        if (suggestionTable == null || suggestionTable.isDisposed()) return;
        int idx = suggestionTable.getSelectionIndex();
        if (idx < 0) return;

        TableItem item = suggestionTable.getItem(idx);
        QuickFixEngine.Suggestion suggestion = (QuickFixEngine.Suggestion) item.getData();
        if (suggestion == null) return;

        try {
            // Replace the erroneous method name with the suggested one
            styledText.replaceTextRange(errorOffset, errorLength, suggestion.methodName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        dismiss();
    }

    public void dismiss() {
        if (popupShell != null && !popupShell.isDisposed()) {
            popupShell.setVisible(false);
        }
    }

    public boolean isVisible() {
        return popupShell != null && !popupShell.isDisposed() && popupShell.isVisible();
    }

    private static boolean isDarkTheme(Display display) {
        Color bg = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        double brightness = (bg.getRed() * 299.0 + bg.getGreen() * 587.0 + bg.getBlue() * 114.0) / 1000.0;
        return brightness < 128;
    }
}
