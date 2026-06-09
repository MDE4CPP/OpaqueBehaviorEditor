package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EOperation;

/**
 * Utility class to traverse an Ecore Model and harvest elements (EClasses, EStructuralFeatures, EOperations)
 * to populate the dictionaries used for auto-completion and syntax highlighting.
 */
public class EcoreModelHarvester {

    public static void harvest(EPackage ePackage, Set<String> contextTypes, ModelDictionary dictionary) {
        if (ePackage == null) {
            return;
        }

        TreeIterator<EObject> it = ePackage.eAllContents();
        while (it.hasNext()) {
            EObject obj = it.next();
            
            // 1. Handle general Ecore Types
            if (obj instanceof EClassifier t) {
                String typeName = t.getName();
                if (typeName != null && !typeName.isBlank()) {
                    contextTypes.add(typeName);
                    dictionary.autocompleteWords.add(typeName);
                    dictionary.globalElements.put(typeName, t);
                }
            }
            
            // 2. Handle EClass specifically to add MDE4CPP factory "create" methods
            if (obj instanceof EClass c) {
                String className = c.getName();
                if (className != null && !className.isBlank()) {
                    dictionary.autocompleteWords.add("create" + className);
                    dictionary.globalElements.put("create" + className, c);
                    
                    Map<String, String> members = dictionary.typeMembers.computeIfAbsent(className, k -> new HashMap<>());
                    Map<String, EObject> elemMembers = dictionary.classElements.computeIfAbsent(className, k -> new HashMap<>());
                    
                    // Register structural features
                    for (EStructuralFeature p : c.getEAllStructuralFeatures()) {
                        String pName = p.getName();
                        if (pName != null && !pName.isBlank()) {
                            String typeName = p.getEType() != null ? p.getEType().getName() : "Object";
                            String retType = typeName;
                            if (p.isMany()) {
                                String col = "Bag";
                                if (p.isOrdered() && p.isUnique()) col = "OrderedSet";
                                else if (p.isOrdered() && !p.isUnique()) col = "Sequence";
                                else if (!p.isOrdered() && p.isUnique()) col = "Set";
                                retType = col + "<" + typeName + ">";
                            }
                            members.put(pName, retType);
                            elemMembers.put(pName, p);
                            String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                            members.put("get" + cap, retType);
                            elemMembers.put("get" + cap, p);
                            members.put("set" + cap, "void");
                            elemMembers.put("set" + cap, p);
                        }
                    }
                    
                    // Register operations
                    for (EOperation op : c.getEAllOperations()) {
                        String opName = op.getName();
                        if (opName != null && !opName.isBlank()) {
                            String typeName = op.getEType() != null ? op.getEType().getName() : "void";
                            String retType = typeName;
                            if (op.isMany()) {
                                String col = "Bag";
                                if (op.isOrdered() && op.isUnique()) col = "OrderedSet";
                                else if (op.isOrdered() && !op.isUnique()) col = "Sequence";
                                else if (!op.isOrdered() && op.isUnique()) col = "Set";
                                retType = col + "<" + typeName + ">";
                            }
                            members.put(opName, retType);
                            elemMembers.put(opName, op);
                        }
                    }
                }
            }
            
            // 4. Register operations as global words
            if (obj instanceof EOperation op) {
                String opName = op.getName();
                if (opName != null && !opName.isBlank()) {
                    dictionary.autocompleteWords.add(opName);
                    dictionary.globalElements.put(opName, op);
                }
            }
            
            // 5. Register features as global words
            if (obj instanceof EStructuralFeature p) {
                String pName = p.getName();
                if (pName != null && !pName.isBlank()) {
                    dictionary.autocompleteWords.add(pName);
                    dictionary.globalElements.put(pName, p);
                    String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                    dictionary.autocompleteWords.add("get" + cap);
                    dictionary.globalElements.put("get" + cap, p);
                    dictionary.autocompleteWords.add("set" + cap);
                    dictionary.globalElements.put("set" + cap, p);
                    
                    String typeName = p.getEType() != null ? p.getEType().getName() : null;
                    String ownerName = p.getEContainingClass() != null ? p.getEContainingClass().getName() : null;
                    
                    if (typeName != null && ownerName != null) {
                        dictionary.autocompleteWords.add("create" + typeName + "_as_" + pName + "_in_" + ownerName);
                        dictionary.globalElements.put("create" + typeName + "_as_" + pName + "_in_" + ownerName, p);
                    }
                }
            }
        }
    }
}
