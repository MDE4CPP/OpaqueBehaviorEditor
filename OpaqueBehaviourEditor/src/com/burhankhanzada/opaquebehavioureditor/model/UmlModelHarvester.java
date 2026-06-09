package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.OpaqueBehavior;

/**
 * Utility class to traverse a UML Model and harvest elements (Classes, Properties, Operations)
 * to populate the dictionaries used for auto-completion and syntax highlighting.
 */
public class UmlModelHarvester extends AbstractModelHarvester {

    /**
     * Traverses the model associated with the given behavior and populates the provided
     * contextTypes set and ModelDictionary.
     *
     * @param behavior     The OpaqueBehavior whose model will be traversed.
     * @param contextTypes A Set to populate with discovered type names.
     * @param dictionary   The dictionary to populate with autocomplete words and elements.
     */
    public static void harvest(OpaqueBehavior behavior, Set<String> contextTypes, ModelDictionary dictionary) {
        if (behavior.getModel() == null) {
            return;
        }

        TreeIterator<EObject> it = behavior.getModel().eAllContents();
        while (it.hasNext()) {
            EObject obj = it.next();
            
            // 1. Handle general UML Types
            if (obj instanceof org.eclipse.uml2.uml.Type t) {
                registerType(t.getName(), t, contextTypes, dictionary);
            }
            
            // 2. Handle UML Classes specifically
            if (obj instanceof org.eclipse.uml2.uml.Class c) {
                registerClassCreate(c.getName(), c, dictionary);
            }
            
            // 3. Handle Classifiers to extract their properties and operations
            if (obj instanceof org.eclipse.uml2.uml.Classifier classifier) {
                String className = classifier.getName();
                
                // Register attributes (properties)
                for (org.eclipse.uml2.uml.Property p : classifier.getAllAttributes()) {
                    String typeName = p.getType() != null ? p.getType().getName() : "Object";
                    registerPropertyMember(className, p.getName(), typeName, 
                        p.isMultivalued(), p.isOrdered(), p.isUnique(), p, dictionary);
                }
                
                // Register operations
                for (org.eclipse.uml2.uml.Operation op : classifier.getAllOperations()) {
                    String typeName = op.getType() != null ? op.getType().getName() : "void";
                    
                    org.eclipse.uml2.uml.Parameter retParam = op.getReturnResult();
                    boolean isMany = false;
                    boolean isOrdered = false;
                    boolean isUnique = false;
                    if (retParam != null) {
                        isMany = retParam.isMultivalued();
                        isOrdered = retParam.isOrdered();
                        isUnique = retParam.isUnique();
                    }
                    
                    registerOperationMember(className, op.getName(), typeName, 
                        isMany, isOrdered, isUnique, op, dictionary);
                }
            }
            
            // 4. Register operations as global words
            if (obj instanceof org.eclipse.uml2.uml.Operation op) {
                registerGlobalOperation(op.getName(), op, dictionary);
            }
            
            // 5. Register properties as global words
            if (obj instanceof org.eclipse.uml2.uml.Property p) {
                String typeName = p.getType() != null ? p.getType().getName() : null;
                String ownerName = null;
                
                // Determine the correct owner name for the property.
                if (p.getClass_() != null) {
                    ownerName = p.getClass_().getName();
                } else if (p.getAssociation() != null) {
                    for (org.eclipse.uml2.uml.Property end : p.getAssociation().getMemberEnds()) {
                        if (end != p && end.getType() != null) {
                            ownerName = end.getType().getName();
                            break;
                        }
                    }
                } else if (p.getOwner() instanceof org.eclipse.uml2.uml.NamedElement ne) {
                    ownerName = ne.getName();
                }
                
                registerGlobalFeature(p.getName(), typeName, ownerName, p, dictionary);
            }
        }
    }
}
