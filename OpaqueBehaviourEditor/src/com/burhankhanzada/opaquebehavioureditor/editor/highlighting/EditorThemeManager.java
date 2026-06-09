package com.burhankhanzada.opaquebehavioureditor.editor.highlighting;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.burhankhanzada.opaquebehavioureditor.ui.ThemeUtils;

public class EditorThemeManager {

    private Font monoFont;
    private Color umlTypeColor;
    private Color methodColor;
    private Color variableColor;
    private Color keywordColor;
    private Color commentColor;
    private Color stringColor;
    
    private Color searchHighlightColor;
    private String currentSearchText;
    
    private int currentFontSize = 13;
    private final SourceViewer sourceViewer;

    public EditorThemeManager(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void setupEditorFontAndColors(Composite parent, StyledText codeText) {
        Display display = parent.getDisplay();
        
        String fontName = System.getProperty("os.name").toLowerCase().contains("mac") ? "Monaco" : "Consolas";
        monoFont = new Font(display, fontName, currentFontSize, SWT.NORMAL);
        codeText.setFont(monoFont);
        codeText.setTabs(4);
        
        codeText.setMargins(45, 5, 5, 5);

        boolean dark = ThemeUtils.isDarkTheme(parent);
        final Color lineNumColor;
        final Color separatorColor;
        
        if (dark) {
            Color darkBg = new Color(display, new RGB(30, 30, 30));
            Color darkFg = new Color(display, new RGB(212, 212, 212));
            Color darkSelBg = new Color(display, new RGB(38, 79, 120));
            Color darkSelFg = new Color(display, new RGB(255, 255, 255));
            
            codeText.setData("org.eclipse.e4.ui.css.id", "UMLOpaqueBehaviourEditorText");
            
            codeText.setBackground(darkBg);
            codeText.setForeground(darkFg);
            codeText.setSelectionBackground(darkSelBg);
            codeText.setSelectionForeground(darkSelFg);
            
            display.asyncExec(() -> {
                if (codeText != null && !codeText.isDisposed()) {
                    codeText.setBackground(darkBg);
                    codeText.setForeground(darkFg);
                    codeText.setSelectionBackground(darkSelBg);
                    codeText.setSelectionForeground(darkSelFg);
                }
            });
            
            lineNumColor = new Color(display, new RGB(133, 133, 133));
            separatorColor = new Color(display, new RGB(80, 80, 80));
            searchHighlightColor = new Color(display, new RGB(100, 100, 0)); 
            
            umlTypeColor = new Color(display, 78, 201, 176);
            methodColor = new Color(display, 220, 220, 170);
            variableColor = new Color(display, 156, 220, 254);
            keywordColor = new Color(display, 197, 134, 192); 
            commentColor = new Color(display, 96, 139, 78);   
            stringColor = new Color(display, 206, 145, 120);  
        } else {
            lineNumColor = new Color(display, new RGB(43, 145, 175));
            separatorColor = new Color(display, new RGB(200, 200, 200));
            searchHighlightColor = new Color(display, new RGB(255, 255, 0)); 
            
            umlTypeColor = new Color(display, 38, 127, 153); 
            methodColor = new Color(display, 121, 94, 38);   
            variableColor = new Color(display, 0, 16, 128);  
            keywordColor = new Color(display, 175, 0, 219);  
            commentColor = new Color(display, 0, 128, 0);    
            stringColor = new Color(display, 163, 21, 21);   
        }

        codeText.addDisposeListener(e -> {
            if (lineNumColor != null) lineNumColor.dispose();
            if (separatorColor != null) separatorColor.dispose();
            if (umlTypeColor != null) umlTypeColor.dispose();
            if (methodColor != null) methodColor.dispose();
            if (variableColor != null) variableColor.dispose();
            if (keywordColor != null) keywordColor.dispose();
            if (commentColor != null) commentColor.dispose();
            if (stringColor != null) stringColor.dispose();
            if (searchHighlightColor != null) searchHighlightColor.dispose();
        });
        
        codeText.setData("lineNumColor", lineNumColor);
        codeText.setData("separatorColor", separatorColor);
    }

    public void setupBracketMatching() {
        org.eclipse.jface.text.source.DefaultCharacterPairMatcher matcher = 
            new org.eclipse.jface.text.source.DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
        
        org.eclipse.jface.text.source.MatchingCharacterPainter painter = 
            new org.eclipse.jface.text.source.MatchingCharacterPainter(sourceViewer, matcher);
        
        Color matchColor = new Color(sourceViewer.getTextWidget().getDisplay(), new RGB(160, 160, 160));
        painter.setColor(matchColor);
        painter.setHighlightCharacterAtCaretLocation(true);
        
        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension2 ext2) {
            ext2.addPainter(painter);
        }
        
        sourceViewer.getTextWidget().addDisposeListener(e -> {
            if (matchColor != null) matchColor.dispose();
            if (matcher != null) matcher.dispose();
            if (painter != null) painter.deactivate(true);
        });
    }

    public void setupCurrentLineHighlighting() {
        org.eclipse.jface.text.CursorLinePainter cursorPainter = 
            new org.eclipse.jface.text.CursorLinePainter(sourceViewer);
            
        Color cursorLineColor = new Color(sourceViewer.getTextWidget().getDisplay(), new RGB(40, 40, 40));
        cursorPainter.setHighlightColor(cursorLineColor);
        
        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension2 ext2) {
            ext2.addPainter(cursorPainter);
        }
        
        sourceViewer.getTextWidget().addDisposeListener(e -> {
            if (cursorLineColor != null) cursorLineColor.dispose();
            if (cursorPainter != null) cursorPainter.deactivate(true);
        });
    }

    public void zoomIn() {
        if (currentFontSize < 40) {
            currentFontSize++;
            updateFont();
        }
    }

    public void zoomOut() {
        if (currentFontSize > 6) {
            currentFontSize--;
            updateFont();
        }
    }

    private void updateFont() {
        if (sourceViewer == null || sourceViewer.getTextWidget() == null || sourceViewer.getTextWidget().isDisposed()) return;
        StyledText codeText = sourceViewer.getTextWidget();
        Display display = codeText.getDisplay();
        
        Font oldFont = monoFont;
        String fontName = System.getProperty("os.name").toLowerCase().contains("mac") ? "Monaco" : "Consolas";
        monoFont = new Font(display, fontName, currentFontSize, SWT.NORMAL);
        codeText.setFont(monoFont);
        
        if (oldFont != null && !oldFont.isDisposed()) {
            oldFont.dispose();
        }
    }

    public void highlightSearch(String text) {
        this.currentSearchText = text;
        if (sourceViewer != null && sourceViewer.getTextWidget() != null && !sourceViewer.getTextWidget().isDisposed()) {
            sourceViewer.invalidateTextPresentation();
        }
    }

    public void dispose() {
        if (monoFont != null && !monoFont.isDisposed()) monoFont.dispose();
        if (searchHighlightColor != null && !searchHighlightColor.isDisposed()) searchHighlightColor.dispose();
    }

    public Color getUmlTypeColor() { return umlTypeColor; }
    public Color getMethodColor() { return methodColor; }
    public Color getVariableColor() { return variableColor; }
    public Color getKeywordColor() { return keywordColor; }
    public Color getCommentColor() { return commentColor; }
    public Color getStringColor() { return stringColor; }
    public Color getSearchHighlightColor() { return searchHighlightColor; }
    public String getCurrentSearchText() { return currentSearchText; }
}
