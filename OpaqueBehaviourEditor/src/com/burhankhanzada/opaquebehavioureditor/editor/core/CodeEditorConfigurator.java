package com.burhankhanzada.opaquebehavioureditor.editor.core;

import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.tm4e.core.grammar.IGrammar;
import org.eclipse.tm4e.registry.TMEclipseRegistryPlugin;
import org.eclipse.tm4e.ui.text.TMPresentationReconciler;

import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;
import com.burhankhanzada.opaquebehavioureditor.editor.actions.EditorActionManager;
import com.burhankhanzada.opaquebehavioureditor.editor.actions.FoldingAction;
import com.burhankhanzada.opaquebehavioureditor.editor.folding.FoldingManager;
import com.burhankhanzada.opaquebehavioureditor.editor.folding.FoldingRulerColumn;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.EditorThemeManager;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.SemanticHighlighter;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.SemanticPresentationListener;
import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.SemanticTextHover;
import com.burhankhanzada.opaquebehavioureditor.editor.text.SmartAutoEditStrategy;
import com.burhankhanzada.opaquebehavioureditor.editor.ui.LineNumberPainter;
import com.burhankhanzada.opaquebehavioureditor.editor.ui.SimpleFindReplaceDialog;

public class CodeEditorConfigurator {

    private final SemanticHighlighter semanticHighlighter;
    private final ModelValidator modelValidator;
    
    private TMPresentationReconciler tmReconciler;
    private EditorThemeManager themeManager;
    private EditorActionManager actionManager;
    private FoldingManager foldingManager;
    private FoldingAction foldingAction;

    public CodeEditorConfigurator(SemanticHighlighter semanticHighlighter, ModelValidator modelValidator) {
        this.semanticHighlighter = semanticHighlighter;
        this.modelValidator = modelValidator;
    }

    public void configure(Composite parent, SourceViewer sourceViewer) {
        StyledText codeText = sourceViewer.getTextWidget();

        // Prevent ESC from closing the dialog while in the code editor
        codeText.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_ESCAPE) {
                e.doit = false;
            }
        });

        // Delegate theming and visual setup
        this.themeManager = new EditorThemeManager(sourceViewer);
        themeManager.setupEditorFontAndColors(parent, codeText);
        themeManager.setupBracketMatching();
        themeManager.setupCurrentLineHighlighting();

        setupLineNumbers(codeText);
        setupFolding(sourceViewer, codeText);
        setupSyntaxHighlighting(sourceViewer, codeText);

        // ---- Attach Undo/Redo Manager ----
        org.eclipse.jface.text.IUndoManager undoManager = new org.eclipse.jface.text.TextViewerUndoManager(200);
        undoManager.connect(sourceViewer);
        
        SimpleFindReplaceDialog findDialog = new SimpleFindReplaceDialog(parent.getShell(), sourceViewer.getFindReplaceTarget(), this);
        
        // Delegate keyboard shortcuts and actions
        this.actionManager = new EditorActionManager(sourceViewer, undoManager, findDialog, themeManager, foldingAction);
        codeText.addVerifyKeyListener(actionManager.createVerifyKeyListener());
    }

    private void setupLineNumbers(StyledText codeText) {
        org.eclipse.swt.graphics.Color lineNumColor = (org.eclipse.swt.graphics.Color) codeText.getData("lineNumColor");
        org.eclipse.swt.graphics.Color separatorColor = (org.eclipse.swt.graphics.Color) codeText.getData("separatorColor");
        
        codeText.addPaintListener(new LineNumberPainter(codeText, lineNumColor, separatorColor));
    }

    private void setupFolding(SourceViewer sourceViewer, StyledText codeText) {
        this.foldingManager = new FoldingManager(codeText);
        
        // Create the fold icon ruler column
        org.eclipse.swt.graphics.Color foldColor = themeManager.getFoldIconColor();
        org.eclipse.swt.graphics.Color foldHoverColor = themeManager.getFoldIconHoverColor();
        FoldingRulerColumn foldingRuler = new FoldingRulerColumn(codeText, foldingManager, foldColor, foldHoverColor);
        codeText.addPaintListener(foldingRuler);
        
        // Create keyboard action handler
        this.foldingAction = new FoldingAction(sourceViewer, foldingManager);
        
        // Recompute regions after text modifications (with a small delay to batch changes)
        codeText.addModifyListener(e -> {
            codeText.getDisplay().timerExec(100, () -> {
                if (!codeText.isDisposed() && !foldingManager.hasCollapsedRegions()) {
                    foldingManager.recomputeRegions();
                    codeText.redraw();
                }
            });
        });
        
        // Initial region computation
        codeText.getDisplay().asyncExec(() -> {
            if (!codeText.isDisposed()) {
                foldingManager.recomputeRegions();
                codeText.redraw();
            }
        });
    }

    private void setupSyntaxHighlighting(SourceViewer sourceViewer, StyledText codeText) {
        try {
            tmReconciler = new TMPresentationReconciler();
            sourceViewer.configure(new SourceViewerConfiguration() {
                @Override
                public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
                    return tmReconciler;
                }
                
                @Override
                public org.eclipse.jface.text.IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
                    return new org.eclipse.jface.text.IAutoEditStrategy[] { new SmartAutoEditStrategy() };
                }
                
                @Override
                public org.eclipse.jface.text.ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
                    return new SemanticTextHover(modelValidator.getDictionary());
                }
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }

        if (sourceViewer instanceof org.eclipse.jface.text.ITextViewerExtension4 ext4) {
            SemanticPresentationListener listener = new SemanticPresentationListener(
                codeText, semanticHighlighter, modelValidator, themeManager
            );
            ext4.addTextPresentationListener(listener);
        }
    }

    public void updateSyntaxLanguage(String lang, StyledText codeText) {
        if (codeText != null && !codeText.isDisposed()) {
            codeText.setData("currentLanguage", lang);
        }
        
        if (tmReconciler == null) return;
        try {
            String lower = lang == null ? "" : lang.toLowerCase();
            String ext = "txt";
            if (lower.startsWith("c++") || lower.equals("cpp")) ext = "cpp";
            else if (lower.equals("java")) ext = "java";
            else if (lower.equals("python") || lower.equals("py")) ext = "py";
            else if (lower.equals("c") || lower.matches("c[0-9]+")) ext = "c";
            
            IGrammar grammar = TMEclipseRegistryPlugin.getGrammarRegistryManager().getGrammarForFileExtension(ext);
            tmReconciler.setGrammar(grammar);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void dispose() {
        if (themeManager != null) {
            themeManager.dispose();
        }
        if (tmReconciler != null) {
            tmReconciler.uninstall();
        }
    }
    
    public void highlightSearch(String text) {
        if (themeManager != null) {
            themeManager.highlightSearch(text);
        }
    }

    /**
     * Returns the FoldingManager for this editor, or null if not yet initialized.
     */
    public FoldingManager getFoldingManager() {
        return foldingManager;
    }
}
