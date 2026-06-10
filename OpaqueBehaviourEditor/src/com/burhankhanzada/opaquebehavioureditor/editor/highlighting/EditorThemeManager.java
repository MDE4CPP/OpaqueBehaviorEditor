package com.burhankhanzada.opaquebehavioureditor.editor.highlighting;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.FontDescriptor;

import com.burhankhanzada.opaquebehavioureditor.ui.ThemeUtils;

public class EditorThemeManager {

    private LocalResourceManager resourceManager;

    private Font monoFont;
    private Color umlTypeColor;
    private Color methodColor;
    private Color variableColor;
    private Color keywordColor;
    private Color commentColor;
    private Color stringColor;
    
    private Color foldIconColor;
    private Color foldIconHoverColor;
    
    private Color searchHighlightColor;
    private String currentSearchText;
    
    private int currentFontSize = 13;
    private final SourceViewer sourceViewer;

    public EditorThemeManager(SourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    public void setupEditorFontAndColors(Composite parent, StyledText codeText) {
        Display display = parent.getDisplay();
        
        // Initialize the resource manager tied to the text widget
        this.resourceManager = new LocalResourceManager(JFaceResources.getResources(), codeText);
        
        String fontName = System.getProperty("os.name").toLowerCase().contains("mac") ? "Monaco" : "Consolas";
        monoFont = (Font) resourceManager.create(FontDescriptor.createFrom(fontName, currentFontSize, SWT.NORMAL));
        codeText.setFont(monoFont);
        codeText.setTabs(4);
        
        codeText.setMargins(60, 5, 5, 5);

        boolean dark = ThemeUtils.isDarkTheme(parent);
        final Color lineNumColor;
        final Color separatorColor;
        
        if (dark) {
            Color darkBg = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(30, 30, 30)));
            Color darkFg = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(212, 212, 212)));
            Color darkSelBg = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(38, 79, 120)));
            Color darkSelFg = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(255, 255, 255)));
            
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
            
            lineNumColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(133, 133, 133)));
            separatorColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(80, 80, 80)));
            searchHighlightColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(100, 100, 0)));
            foldIconColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(140, 140, 140)));
            foldIconHoverColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(200, 200, 200)));
            
            umlTypeColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(78, 201, 176)));
            methodColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(220, 220, 170)));
            variableColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(156, 220, 254)));
            keywordColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(197, 134, 192))); 
            commentColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(96, 139, 78)));   
            stringColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(206, 145, 120)));  
        } else {
            lineNumColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(43, 145, 175)));
            separatorColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(200, 200, 200)));
            searchHighlightColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(255, 255, 0)));
            foldIconColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(120, 120, 120)));
            foldIconHoverColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(60, 60, 60)));
            
            umlTypeColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(38, 127, 153))); 
            methodColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(121, 94, 38)));   
            variableColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(0, 16, 128)));  
            keywordColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(175, 0, 219)));  
            commentColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(0, 128, 0)));    
            stringColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(163, 21, 21)));   
        }

        // No need for addDisposeListener because resourceManager handles it automatically
        
        codeText.setData("lineNumColor", lineNumColor);
        codeText.setData("separatorColor", separatorColor);
    }

    public void setupBracketMatching() {
        org.eclipse.jface.text.source.DefaultCharacterPairMatcher matcher = 
            new org.eclipse.jface.text.source.DefaultCharacterPairMatcher(new char[] { '{', '}', '(', ')', '[', ']' });
        
        org.eclipse.jface.text.source.MatchingCharacterPainter painter = 
            new org.eclipse.jface.text.source.MatchingCharacterPainter(sourceViewer, matcher);
        
        Color matchColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(160, 160, 160)));
        painter.setColor(matchColor);
        painter.setHighlightCharacterAtCaretLocation(true);
        
        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension2 ext2) {
            ext2.addPainter(painter);
        }
        
        sourceViewer.getTextWidget().addDisposeListener(e -> {
            if (matcher != null) matcher.dispose();
            if (painter != null) painter.deactivate(true);
        });
    }

    public void setupCurrentLineHighlighting() {
        org.eclipse.jface.text.CursorLinePainter cursorPainter = 
            new org.eclipse.jface.text.CursorLinePainter(sourceViewer);
            
        Color cursorLineColor = (Color) resourceManager.create(ColorDescriptor.createFrom(new RGB(40, 40, 40)));
        cursorPainter.setHighlightColor(cursorLineColor);
        
        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension2 ext2) {
            ext2.addPainter(cursorPainter);
        }
        
        sourceViewer.getTextWidget().addDisposeListener(e -> {
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
        
        String fontName = System.getProperty("os.name").toLowerCase().contains("mac") ? "Monaco" : "Consolas";
        monoFont = (Font) resourceManager.create(FontDescriptor.createFrom(fontName, currentFontSize, SWT.NORMAL));
        codeText.setFont(monoFont);
    }

    public void highlightSearch(String text) {
        this.currentSearchText = text;
        if (sourceViewer != null && sourceViewer.getTextWidget() != null && !sourceViewer.getTextWidget().isDisposed()) {
            sourceViewer.invalidateTextPresentation();
        }
    }

    public void dispose() {
        // Nothing to dispose manually, resourceManager handles everything.
    }

    public Color getUmlTypeColor() { return umlTypeColor; }
    public Color getMethodColor() { return methodColor; }
    public Color getVariableColor() { return variableColor; }
    public Color getKeywordColor() { return keywordColor; }
    public Color getCommentColor() { return commentColor; }
    public Color getStringColor() { return stringColor; }
    public Color getSearchHighlightColor() { return searchHighlightColor; }
    public String getCurrentSearchText() { return currentSearchText; }
    public Color getFoldIconColor() { return foldIconColor; }
    public Color getFoldIconHoverColor() { return foldIconHoverColor; }
}
