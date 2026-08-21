package de.tu_ilmenau.sse.opaquebehavioureditor.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EcorePackage;

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
        // EMF standard methods
        {"eClass", "ecore::EClass"},
        {"eContainer", "ecore::EObject"},
        {"eContents", "Sequence(ecore::EObject)"},
        {"eAllContents", "Sequence(ecore::EObject)"},
        {"eCrossReferences", "Sequence(ecore::EObject)"},
        {"eResource", "ecore::EResource"},
        {"eIsProxy", "Boolean"},
        {"eResolveProxy", "Boolean"},
        {"eGet", "OclAny"},
        {"eSet", "void"},
        {"eIsSet", "Boolean"},
        {"eUnset", "void"},
    };

    /** Source of the annotation MDE4CPP evaluates during generation (keywords.mtl, sourceNameCodeGen). */
    private static final String CODEGEN_SOURCE = "http://tu-ilmenau.de/see/codegen";

    private static boolean hasCodegenKey(EModelElement element, String key) {
        if (element == null) return false;
        org.eclipse.emf.ecore.EAnnotation a = element.getEAnnotation(CODEGEN_SOURCE);
        return a != null && a.getDetails().containsKey(key);
    }

    private static String codegenValue(EModelElement element, String key) {
        org.eclipse.emf.ecore.EAnnotation a = element.getEAnnotation(CODEGEN_SOURCE);
        return a == null ? null : a.getDetails().get(key);
    }

    /**
     * Mirrors doNotGenerateEClassifier / doNotGenerateEPackage: the key excludes the element from
     * generation, and a package carrying it excludes everything inside it.
     */
    private static boolean doNotGenerate(EModelElement element) {
        if (hasCodegenKey(element, "doNotGenerate")) return true;
        for (EObject c = element.eContainer(); c != null; c = c.eContainer()) {
            if (c instanceof EPackage pkg && hasCodegenKey(pkg, "doNotGenerate")) return true;
        }
        return false;
    }

    /** The generated getter name, honouring the getterName override (generateAttribute.mtl). */
    private static String getterName(EStructuralFeature feature) {
        String override = codegenValue(feature, "getterName");
        if (override != null && !override.isBlank()) return override;
        String n = feature.getName();
        return "get" + n.substring(0, 1).toUpperCase() + n.substring(1);
    }

    /**
     * Registers the parameters of the operation whose body is being edited. They are in scope for
     * the body but declared nowhere in it, so without this they are neither proposed nor typed.
     */
    public static void harvestBodyScope(EObject bodyElement, ModelDictionary dictionary) {
        for (EObject c = bodyElement; c != null; c = c.eContainer()) {
            if (!(c instanceof EOperation op)) continue;
            for (org.eclipse.emf.ecore.EParameter param : op.getEParameters()) {
                if (doNotGenerate(param)) continue;
                String typeName = param.getEType() != null ? param.getEType().getName() : "Object";
                registerParameter(param.getName(), typeName, param.isMany(), param, dictionary);
            }
            return;
        }
    }

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
            
            // Elements excluded from generation have no counterpart in the generated C++
            if (obj instanceof EModelElement e && doNotGenerate(e)) {
                if (obj instanceof EClassifier) it.prune();
                continue;
            }

            // 1. Handle general Ecore Types
            if (obj instanceof EClassifier t) {
                registerType(t.getName(), t, contextTypes, dictionary);
            }
            
            // 2. Handle EClass specifically
            if (obj instanceof EClass c) {
                registerClassCreate(c.getName(), c, dictionary);
                
                // Register structural features
                for (EStructuralFeature p : c.getEAllStructuralFeatures()) {
                    if (doNotGenerate(p)) continue;
                    String typeName = p.getEType() != null ? p.getEType().getName() : "Object";
                    registerPropertyMember(c.getName(), p.getName(), typeName, p.isMany(),
                        getterName(p), hasSetter(p.isMany(), p.isChangeable()) && !hasCodegenKey(p, "noSetter"),
                        !hasCodegenKey(p, "noGetter"), p, dictionary);
                }
                
                // Register operations
                for (EOperation op : c.getEAllOperations()) {
                    if (doNotGenerate(op)) continue;
                    String typeName = op.getEType() != null ? op.getEType().getName() : "void";
                    registerOperationMember(c.getName(), op.getName(), typeName, op.isMany(), op, dictionary);
                }
                
                // Register Ecore inheritance (e.g. MyClass -> MySuperClass)
                for (EClass superType : c.getESuperTypes()) {
                    if (superType.getName() != null && !superType.getName().isBlank()) {
                        dictionary.addSuperType(c.getName(), superType.getName());
                    }
                }
                
                // ALL EClasses implicitly inherit from ecore::EObject
                dictionary.addSuperType(c.getName(), "ecore::EObject");

            }
            
            // 4. Register operations as global words
            if (obj instanceof EOperation op) {
                registerGlobalOperation(op.getName(), op, dictionary);
            }

            // 5. Register features as global words
            if (obj instanceof EStructuralFeature p) {
                String typeName = p.getEType() != null ? p.getEType().getName() : null;
                String ownerName = p.getEContainingClass() != null ? p.getEContainingClass().getName() : null;
                boolean containment = p instanceof org.eclipse.emf.ecore.EReference r && r.isContainment();
                registerGlobalFeature(p.getName(), typeName, ownerName, containment, getterName(p),
                        hasSetter(p.isMany(), p.isChangeable()) && !hasCodegenKey(p, "noSetter"),
                        !hasCodegenKey(p, "noGetter"), p, dictionary);
            }
        }

        registerFactoryBuiltins(dictionary);
        harvestMetaclassMethods(dictionary);

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
     * Registers the reflective Ecore metaclass API (ecore::EClass and its supertypes) as its own
     * types. A generated class is an ecore::EObject, not an ecore::EClass, so these members are
     * reached through eClass() - which aql::Services already declares as returning ecore::EClass -
     * and must not be attached to the model classes themselves.
     */
    private static void harvestMetaclassMethods(ModelDictionary dictionary) {
        harvestEClassHierarchy(EcorePackage.eINSTANCE.getEClass(), dictionary, new HashSet<>());
    }

    /**
     * Recursively traverses the metaclass hierarchy and registers the structural features and
     * operations of each metaclass on that metaclass.
     */
    private static void harvestEClassHierarchy(EClass eClass, ModelDictionary dictionary, Set<EClass> visited) {
        if (eClass == null || !visited.add(eClass)) return;

        String metaclassName = buildMetaclassName(eClass);

        for (EStructuralFeature feature : eClass.getEStructuralFeatures()) {
            String featureName = feature.getName();
            String featureType = computeCollectionType(
                    feature.getEType() != null ? feature.getEType().getName() : "Object", feature.isMany());
            dictionary.addTypeMember(metaclassName, featureName, featureType);
            if (featureName != null && !featureName.isBlank()) {
                String cap = featureName.substring(0, 1).toUpperCase() + featureName.substring(1);
                dictionary.addTypeMember(metaclassName, "get" + cap, featureType);
                if (hasSetter(feature.isMany(), feature.isChangeable())) {
                    dictionary.addTypeMember(metaclassName, "set" + cap, "void");
                }
            }
        }

        for (EOperation op : eClass.getEOperations()) {
            String opName = op.getName();
            String retType = op.getEType() != null ? op.getEType().getName() : "void";
            dictionary.addTypeMember(metaclassName, opName, retType);
        }

        for (EClass superEClass : eClass.getESuperTypes()) {
            dictionary.addSuperType(metaclassName, buildMetaclassName(superEClass));
            harvestEClassHierarchy(superEClass, dictionary, visited);
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
