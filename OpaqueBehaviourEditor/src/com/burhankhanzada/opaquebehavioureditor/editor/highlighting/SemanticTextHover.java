package com.burhankhanzada.opaquebehavioureditor.editor.highlighting;

import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;

import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;

public class SemanticTextHover implements ITextHover {

    private final ModelDictionary dictionary;

    public SemanticTextHover(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    @Override
    public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
        try {
            String text = textViewer.getDocument().get(hoverRegion.getOffset(), hoverRegion.getLength());
            if (text == null || text.isBlank()) return null;
            
            EObject element = dictionary.globalElements.get(text);
            if (element != null) {
                return formatHoverInfo(element, text);
            }
            
            // Check if it's a known type
            if (dictionary.typeMembers.containsKey(text)) {
                return "Model Class: " + text;
            }
            
            // Check if it's a method on a known type
            for (Map.Entry<String, Map<String, EObject>> classEntry : dictionary.classElements.entrySet()) {
                if (classEntry.getValue().containsKey(text)) {
                    return formatHoverInfo(classEntry.getValue().get(text), text) + "\n(Member of " + classEntry.getKey() + ")";
                }
            }
            
        } catch (Exception e) {}
        
        return null;
    }
    
    private String formatHoverInfo(EObject element, String name) {
        if (element instanceof EClass) {
            return "Class: " + name;
        } else if (element instanceof EOperation op) {
            String type = op.getEType() != null ? op.getEType().getName() : "void";
            if (op.isMany()) type = "Collection<" + type + ">";
            return "Operation: " + type + " " + name + "()";
        } else if (element instanceof EStructuralFeature sf) {
            String type = sf.getEType() != null ? sf.getEType().getName() : "Object";
            if (sf.isMany()) type = "Collection<" + type + ">";
            return "Feature: " + type + " " + name;
        }
        return "Model Element: " + name;
    }

    @Override
    public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
        try {
            org.eclipse.jface.text.IDocument doc = textViewer.getDocument();
            int start = offset;
            while (start > 0 && Character.isJavaIdentifierPart(doc.getChar(start - 1))) {
                start--;
            }
            int end = offset;
            int length = doc.getLength();
            while (end < length && Character.isJavaIdentifierPart(doc.getChar(end))) {
                end++;
            }
            if (start < end) {
                return new Region(start, end - start);
            }
        } catch (Exception e) {}
        return null;
    }

}
