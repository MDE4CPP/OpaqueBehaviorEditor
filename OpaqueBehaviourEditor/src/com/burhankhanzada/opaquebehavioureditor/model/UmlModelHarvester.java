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
                
                // Register UML model-level generalizations (e.g. Author -> NamedModelElement)
                for (org.eclipse.uml2.uml.Classifier general : classifier.getGenerals()) {
                    if (general.getName() != null && !general.getName().isBlank()) {
                        dictionary.addSuperType(className, general.getName());
                    }
                }
                
                // Harvest methods from the UML metaclass hierarchy (e.g. uml::Classifier, uml::Class).
                // These are runtime methods available in MDE4CPP generated code.
                harvestMetaclassMethods(classifier, className, dictionary);
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

    /**
     * Harvests methods from the UML metaclass hierarchy (e.g. uml::Classifier, uml::Class)
     * for a given classifier. These are runtime methods available in MDE4CPP generated code
     * that are not part of the user model but are inherited from the UML metaclass hierarchy.
     *
     * @param classifier The UML classifier to inspect.
     * @param className  The simple name of the classifier in the user model.
     * @param dictionary The dictionary to populate.
     */
    private static void harvestMetaclassMethods(org.eclipse.uml2.uml.Classifier classifier, 
            String className, ModelDictionary dictionary) {
        if (className == null || className.isBlank()) return;
        
        org.eclipse.emf.ecore.EClass eClass = classifier.eClass();
        Set<org.eclipse.emf.ecore.EClass> visited = new java.util.HashSet<>();
        harvestEClassHierarchy(eClass, className, dictionary, visited);
    }

    /**
     * Recursively traverses the EClass hierarchy and registers structural features
     * and operations from each metaclass, linking them as supertypes of the user class.
     */
    private static void harvestEClassHierarchy(org.eclipse.emf.ecore.EClass eClass, 
            String userClassName, ModelDictionary dictionary, Set<org.eclipse.emf.ecore.EClass> visited) {
        if (eClass == null || !visited.add(eClass)) return;
        
        // Build a qualified metaclass name (e.g. "uml::Class", "uml::Classifier")
        String metaclassName = buildMetaclassName(eClass);
        
        // Link user class -> metaclass as a supertype
        dictionary.addSuperType(userClassName, metaclassName);
        
        // Register structural features of this metaclass
        for (org.eclipse.emf.ecore.EStructuralFeature feature : eClass.getEStructuralFeatures()) {
            String featureName = feature.getName();
            String featureType = feature.getEType() != null ? feature.getEType().getName() : "Object";
            dictionary.addTypeMember(metaclassName, featureName, featureType);
            // Also register getter/setter
            if (featureName != null && !featureName.isBlank()) {
                String cap = featureName.substring(0, 1).toUpperCase() + featureName.substring(1);
                dictionary.addTypeMember(metaclassName, "get" + cap, featureType);
                dictionary.addTypeMember(metaclassName, "set" + cap, "void");
            }
        }
        
        // Register operations of this metaclass
        for (org.eclipse.emf.ecore.EOperation op : eClass.getEOperations()) {
            String opName = op.getName();
            String retType = op.getEType() != null ? op.getEType().getName() : "void";
            dictionary.addTypeMember(metaclassName, opName, retType);
        }
        
        // Recurse into super EClasses and link metaclass inheritance chain
        for (org.eclipse.emf.ecore.EClass superEClass : eClass.getESuperTypes()) {
            String superMetaclassName = buildMetaclassName(superEClass);
            dictionary.addSuperType(metaclassName, superMetaclassName);
            harvestEClassHierarchy(superEClass, userClassName, dictionary, visited);
        }
    }

    /**
     * Builds a qualified metaclass name from an EClass (e.g. "uml::Class", "uml::Classifier").
     */
    private static String buildMetaclassName(org.eclipse.emf.ecore.EClass eClass) {
        String pkgName = eClass.getEPackage() != null ? eClass.getEPackage().getName() : "";
        String className = eClass.getName();
        if (pkgName != null && !pkgName.isBlank()) {
            return pkgName + "::" + className;
        }
        return className;
    }
}
