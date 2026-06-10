package com.burhankhanzada.opaquebehavioureditor.editor.actions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;

import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.source.SourceViewer;

import com.burhankhanzada.opaquebehavioureditor.editor.highlighting.EditorThemeManager;
import com.burhankhanzada.opaquebehavioureditor.editor.ui.SimpleFindReplaceDialog;

public class EditorActionManager {

    public static class KeyBindings {
        public static int UNDO = 'z';
        public static int REDO = 'y';
        public static int FORMAT = 'f';
        public static int FIND = 'f';
        public static int TOGGLE_COMMENT = '/';
        public static int ZOOM_IN_1 = '=';
        public static int ZOOM_IN_2 = '+';
        public static int ZOOM_OUT = '-';
        public static int WORD_WRAP = 'z';
        public static int DELETE_LINE = 'd';
        public static int GO_TO_LINE = 'l';
        public static int DUPLICATE_LINE = SWT.ARROW_DOWN;
        public static int RENAME = SWT.F2;
        public static int RENAME_CTRL = 'r';
    }

    private final SourceViewer sourceViewer;
    private final IUndoManager undoManager;
    private final SimpleFindReplaceDialog findDialog;
    private final EditorThemeManager themeManager;

    public EditorActionManager(SourceViewer sourceViewer, IUndoManager undoManager, 
                               SimpleFindReplaceDialog findDialog, EditorThemeManager themeManager) {
        this.sourceViewer = sourceViewer;
        this.undoManager = undoManager;
        this.findDialog = findDialog;
        this.themeManager = themeManager;
    }

    public VerifyKeyListener createVerifyKeyListener() {
        StyledText codeText = sourceViewer.getTextWidget();
        return new VerifyKeyListener() {
            @Override
            public void verifyKey(VerifyEvent e) {
                boolean isCtrl = (e.stateMask & SWT.MOD1) != 0;
                boolean isShift = (e.stateMask & SWT.SHIFT) != 0;
                boolean isAlt = (e.stateMask & SWT.ALT) != 0;
                
                if (isCtrl && e.keyCode == KeyBindings.UNDO) {
                    if (isShift) {
                        if (undoManager.redoable()) undoManager.redo();
                    } else {
                        if (undoManager.undoable()) undoManager.undo();
                    }
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.REDO) {
                    if (undoManager.redoable()) undoManager.redo();
                    e.doit = false;
                } else if (isCtrl && isShift && e.keyCode == KeyBindings.FORMAT) {
                    new FormatDocumentAction(sourceViewer).execute();
                    e.doit = false;
                } else if (isCtrl && !isShift && e.keyCode == KeyBindings.FIND) {
                    findDialog.open();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.TOGGLE_COMMENT) {
                    new ToggleCommentsAction(sourceViewer).execute();
                    e.doit = false;
                } else if (isCtrl && (e.keyCode == KeyBindings.ZOOM_IN_1 || e.keyCode == KeyBindings.ZOOM_IN_2)) {
                    themeManager.zoomIn();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.ZOOM_OUT) {
                    themeManager.zoomOut();
                    e.doit = false;
                } else if (isAlt && e.keyCode == KeyBindings.WORD_WRAP) {
                    codeText.setWordWrap(!codeText.getWordWrap());
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.DELETE_LINE) {
                    new DeleteLineAction(sourceViewer).execute();
                    e.doit = false;
                } else if (isCtrl && e.keyCode == KeyBindings.GO_TO_LINE) {
                    new GoToLineAction(sourceViewer).execute();
                    e.doit = false;
                } else if (isCtrl && isAlt && e.keyCode == KeyBindings.DUPLICATE_LINE) {
                    new DuplicateLineAction(sourceViewer).execute();
                    e.doit = false;
                } else if (e.keyCode == KeyBindings.RENAME || (isCtrl && e.keyCode == KeyBindings.RENAME_CTRL)) {
                    new RenameSymbolAction(sourceViewer).execute();
                    e.doit = false;
                } else if (e.keyCode == SWT.TAB) {
                    if (isShift) {
                        sourceViewer.doOperation(ITextOperationTarget.SHIFT_LEFT);
                        e.doit = false;
                    } else if (codeText.getSelectionCount() > 0) {
                        sourceViewer.doOperation(ITextOperationTarget.SHIFT_RIGHT);
                        e.doit = false;
                    }
                }
            }
        };
    }
}
