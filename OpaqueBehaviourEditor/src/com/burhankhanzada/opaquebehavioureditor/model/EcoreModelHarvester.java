package com.burhankhanzada.opaquebehavioureditor.model;

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
public class EcoreModelHarvester extends AbstractModelHarvester {

    public static void harvest(EPackage ePackage, Set<String> contextTypes, ModelDictionary dictionary) {
        if (ePackage == null) {
            return;
        }
        
        if (ePackage.getName() != null && !ePackage.getName().isBlank()) {
            dictionary.addTypeMemberMap(ePackage.getName());
            dictionary.addTypeMemberMap(ePackage.getName() + "_ecore");
        }

        TreeIterator<EObject> it = ePackage.eAllContents();
        while (it.hasNext()) {
            EObject obj = it.next();
            
            // 1. Handle general Ecore Types
            if (obj instanceof EClassifier t) {
                registerType(t.getName(), t, contextTypes, dictionary);
            }
            
            // 2. Handle EClass specifically
            if (obj instanceof EClass c) {
                registerClassCreate(c.getName(), c, dictionary);
                
                // Register structural features
                for (EStructuralFeature p : c.getEAllStructuralFeatures()) {
                    String typeName = p.getEType() != null ? p.getEType().getName() : "Object";
                    registerPropertyMember(c.getName(), p.getName(), typeName, 
                        p.isMany(), p.isOrdered(), p.isUnique(), p, dictionary);
                }
                
                // Register operations
                for (EOperation op : c.getEAllOperations()) {
                    String typeName = op.getEType() != null ? op.getEType().getName() : "void";
                    registerOperationMember(c.getName(), op.getName(), typeName, 
                        op.isMany(), op.isOrdered(), op.isUnique(), op, dictionary);
                }
            }
            
            // 4. Register operations as global words
            if (obj instanceof EOperation op) {
                registerGlobalOperation(op.getName(), op, dictionary);
            }
            
            // 5. Register features as global words
            if (obj instanceof EStructuralFeature p) {
                String typeName = p.getEType() != null ? p.getEType().getName() : null;
                String ownerName = p.getEContainingClass() != null ? p.getEContainingClass().getName() : null;
                registerGlobalFeature(p.getName(), typeName, ownerName, p, dictionary);
            }
        }
    }
}
