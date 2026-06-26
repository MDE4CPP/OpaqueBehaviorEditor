package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.HashSet;
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

    /** AQL/OCL built-in service methods available on all EObjects. */
    private static final String AQL_SERVICES_TYPE = "aql::Services";
    private static final String[][] AQL_SERVICES = {
        // AQL navigation services
        {"ancestors", "Sequence(OclAny)"},
        {"current", "OclAny"},
        {"eAllContents", "Sequence(OclAny)"},
        {"followingSiblings", "Sequence(OclAny)"},
        {"precedingSiblings", "Sequence(OclAny)"},
        {"siblings", "Sequence(OclAny)"},
        // OCL standard library
        {"oclAsSet", "Set(EObject)"},
        {"oclAsType", "T"},
        {"oclIsInState", "Boolean"},
        {"oclIsInvalid", "Boolean"},
        {"oclIsKindOf", "Boolean"},
        {"oclIsTypeOf", "Boolean"},
        {"oclIsUndefined", "Boolean"},
        // AQL/Java utility services
        {"invoke", "Sequence(OclAny)"},
        {"isInstance", "Boolean"},
        {"isSuperTypeOf", "Boolean"},
        {"lineSeparator", "String"},
        {"toString", "String"},
        {"getProperty", "String"},
    };

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
                
                // Register Ecore inheritance (e.g. MyClass -> MySuperClass)
                for (EClass superType : c.getESuperTypes()) {
                    if (superType.getName() != null && !superType.getName().isBlank()) {
                        dictionary.addSuperType(c.getName(), superType.getName());
                    }
                }

                // Harvest methods from the Ecore metaclass hierarchy (e.g. ecore::EClass, ecore::EObject).
                // These are runtime methods available on all EObjects in EMF.
                harvestMetaclassMethods(c, c.getName(), dictionary);
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

        // Register AQL/OCL built-in services on a synthetic type
        registerAqlServices(dictionary);

        // Register context variables (self, thisEObject, currentModel)
        dictionary.addAutocompleteWord("self");
        dictionary.addAutocompleteWord("thisEObject");
        dictionary.addAutocompleteWord("currentModel");
    }

    /**
     * Registers AQL/OCL built-in services on a synthetic type and links it
     * to ecore::EObject so all model types inherit these services.
     */
    private static void registerAqlServices(ModelDictionary dictionary) {
        for (String[] service : AQL_SERVICES) {
            dictionary.addTypeMember(AQL_SERVICES_TYPE, service[0], service[1]);
            dictionary.addAutocompleteWord(service[0]);
        }
        // Link ecore::EObject -> aql::Services so all types inherit them
        dictionary.addSuperType("ecore::EObject", AQL_SERVICES_TYPE);
    }

    /**
     * Harvests methods from the Ecore metaclass hierarchy (e.g. ecore::EClass, ecore::EObject)
     * for a given EClass. These are runtime methods available on all EObjects in EMF
     * that are not part of the user model but are inherited from the Ecore metaclass hierarchy.
     */
    private static void harvestMetaclassMethods(EClass eClass, String className, ModelDictionary dictionary) {
        if (className == null || className.isBlank()) return;

        EClass metaEClass = eClass.eClass();
        Set<EClass> visited = new HashSet<>();
        harvestEClassHierarchy(metaEClass, className, dictionary, visited);
    }

    /**
     * Recursively traverses the EClass hierarchy and registers structural features
     * and operations from each metaclass, linking them as supertypes of the user class.
     */
    private static void harvestEClassHierarchy(EClass eClass, String userClassName,
            ModelDictionary dictionary, Set<EClass> visited) {
        if (eClass == null || !visited.add(eClass)) return;

        String metaclassName = buildMetaclassName(eClass);

        dictionary.addSuperType(userClassName, metaclassName);

        for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
            String featureName = feature.getName();
            String featureType = feature.getEType() != null ? feature.getEType().getName() : "Object";
            dictionary.addTypeMember(metaclassName, featureName, featureType);
            if (featureName != null && !featureName.isBlank()) {
                String cap = featureName.substring(0, 1).toUpperCase() + featureName.substring(1);
                dictionary.addTypeMember(metaclassName, "get" + cap, featureType);
                dictionary.addTypeMember(metaclassName, "set" + cap, "void");
            }
        }

        for (EOperation op : eClass.getEOperations()) {
            String opName = op.getName();
            String retType = op.getEType() != null ? op.getEType().getName() : "void";
            dictionary.addTypeMember(metaclassName, opName, retType);
        }

        for (EClass superEClass : eClass.getESuperTypes()) {
            String superMetaclassName = buildMetaclassName(superEClass);
            dictionary.addSuperType(metaclassName, superMetaclassName);
            harvestEClassHierarchy(superEClass, userClassName, dictionary, visited);
        }
    }

    /**
     * Builds a qualified metaclass name from an EClass (e.g. "ecore::EClass").
     */
    private static String buildMetaclassName(EClass eClass) {
        String pkgName = eClass.getEPackage() != null ? eClass.getEPackage().getName() : "";
        String className = eClass.getName();
        if (pkgName != null && !pkgName.isBlank()) {
            return pkgName + "::" + className;
        }
        return className;
    }
}
