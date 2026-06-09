package com.burhankhanzada.opaquebehavioureditor.editor.ui;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;

import com.burhankhanzada.opaquebehavioureditor.editor.text.CppExpressionParser;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.TextUtilities;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.utils.PluginLogger;

public class EditorInteractionHandler {

    private final StyledText styledText;
    private final ModelDictionary dictionary;
    private ISelectionProvider selectionProvider;
    private LanguageDef currentLangDef;

    public EditorInteractionHandler(StyledText styledText, ModelDictionary dictionary) {
        this.styledText = styledText;
        this.dictionary = dictionary;
        setupHoverTooltips();
        setupHyperlinkNavigation();
    }

    public void setSelectionProvider(ISelectionProvider provider) {
        this.selectionProvider = provider;
    }

    public void setLanguage(LanguageDef currentLangDef) {
        this.currentLangDef = currentLangDef;
    }

    private void setupHoverTooltips() {
        styledText.addMouseMoveListener(e -> {
            boolean isMod = (e.stateMask & SWT.MOD1) != 0;
            boolean hasHyperlink = false;
            try {
                int offset = styledText.getOffsetAtPoint(new Point(e.x, e.y));
                String text = styledText.getText();
                int[] bounds = TextUtilities.getWordBounds(text, offset);
                int start = bounds[0];
                int end = bounds[1];
                
                if (start < end) {
                    String word = text.substring(start, end);
                    String textBefore = TextUtilities.getTextBeforeIdentifier(text, offset);
                    
                    EObject hyperlinkObj = resolveHyperlink(word, textBefore);
                    if (hyperlinkObj != null) {
                        hasHyperlink = true;
                    }

                    String type = resolveVariableType(word);
                    if (type != null && currentLangDef != null && currentLangDef.name.equals(LanguageMapping.LANG_CPP)) {
                        styledText.setToolTipText("std::shared_ptr<" + type + ">");
                    } else if (type != null) {
                        styledText.setToolTipText(type);
                    } else {
                        styledText.setToolTipText(null);
                    }
                } else {
                    styledText.setToolTipText(null);
                }
            } catch (IllegalArgumentException ex) {
                PluginLogger.logWarning("Tooltip calculation failed at hover position");
                styledText.setToolTipText(null);
            }

            if (isMod && hasHyperlink) {
                styledText.setCursor(styledText.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
            } else {
                styledText.setCursor(null);
            }
        });
    }

    private void setupHyperlinkNavigation() {
        styledText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if ((e.keyCode & SWT.MODIFIER_MASK) == SWT.MOD1 || e.keyCode == SWT.COMMAND || e.keyCode == SWT.CTRL) {
                    styledText.setCursor(null);
                }
            }
        });

        styledText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if ((e.stateMask & SWT.MOD1) != 0 && selectionProvider != null) {
                    try {
                        int offset = styledText.getOffsetAtPoint(new Point(e.x, e.y));
                        String text = styledText.getText();
                        int[] bounds = TextUtilities.getWordBounds(text, offset);
                        int start = bounds[0];
                        int end = bounds[1];
                        
                        if (start < end) {
                            String word = text.substring(start, end);
                            String textBefore = TextUtilities.getTextBeforeIdentifier(text, offset);
                            EObject obj = resolveHyperlink(word, textBefore);
                            if (obj != null) {
                                selectionProvider.setSelection(new StructuredSelection(obj));
                            }
                        }
                    } catch (IllegalArgumentException ex) {
                        PluginLogger.logWarning("Navigation failed at click position");
                    }
                }
            }
        });
    }

    private EObject resolveHyperlink(String word, String textBeforeCaret) {
        String contextType = CppExpressionParser.resolveContextTypeFromText(textBeforeCaret, dictionary, styledText.getText());
        if (contextType != null) {
            if (contextType.startsWith("std::shared_ptr<")) {
                contextType = contextType.substring(16, contextType.length() - 1);
            }
            if (dictionary.classElements.containsKey(contextType)) {
                return dictionary.classElements.get(contextType).get(word);
            }
        } else {
            return dictionary.globalElements.get(word);
        }
        return null;
    }

    private String resolveVariableType(String variableName) {
        return CppExpressionParser.resolveVariableType(variableName, styledText.getText());
    }
}
