package de.tu_ilmenau.sse.opaquebehavioureditor.model;

import java.util.Set;
import org.eclipse.emf.ecore.EObject;

/**
 * Shared base class containing common logic for harvesting UML/Ecore models.
 */
public abstract class AbstractModelHarvester {

    protected static void registerType(String typeName, EObject obj, Set<String> contextTypes, ModelDictionary dictionary) {
        if (typeName != null && !typeName.isBlank()) {
            contextTypes.add(typeName);
            dictionary.addAutocompleteWord(typeName);
            dictionary.addGlobalElement(typeName, obj);
        }
    }

    /** Synthetic type carrying the generated factory's create* methods (see MDE4CPP <model>Factory.hpp). */
    public static final String FACTORY_TYPE = "factory";

    /** Members every generated <model>Factory declares, independent of the model's classes. */
    protected static void registerFactoryBuiltins(ModelDictionary dictionary) {
        dictionary.addTypeMember(FACTORY_TYPE, "create", "std::shared_ptr<ecore::EObject>");
        dictionary.addTypeMember(FACTORY_TYPE, "eInstance", FACTORY_TYPE);
    }

    protected static void registerClassCreate(String className, EObject obj, ModelDictionary dictionary) {
        if (className != null && !className.isBlank()) {
            dictionary.addAutocompleteWord("create" + className);
            dictionary.addGlobalElement("create" + className, obj);
            dictionary.addTypeMember(FACTORY_TYPE, "create" + className, "std::shared_ptr<" + className + ">");
        }
    }

    protected static void registerPropertyMember(String className, String pName, String typeName, 
            boolean isMany, boolean isOrdered, boolean isUnique, boolean hasSetter, EObject p, ModelDictionary dictionary) {
        if (className == null || className.isBlank() || pName == null || pName.isBlank()) return;
        
        String retType = computeCollectionType(typeName, isMany, isOrdered, isUnique);
        
        dictionary.addTypeMember(className, pName, retType);
        dictionary.addClassElement(className, pName, p);
        String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
        dictionary.addTypeMember(className, "get" + cap, retType);
        dictionary.addClassElement(className, "get" + cap, p);
        if (hasSetter) {
            dictionary.addTypeMember(className, "set" + cap, "void");
            dictionary.addClassElement(className, "set" + cap, p);
        }
    }

    /**
     * MDE4CPP generates a setter only for changeable, single-valued features
     * (hasSetter in generateAttribute.mtl / generateReference.mtl). Multi-valued
     * features are mutated through the collection returned by their getter.
     */
    protected static boolean hasSetter(boolean isMany, boolean isChangeable) {
        return !isMany && isChangeable;
    }
    
    protected static void registerOperationMember(String className, String opName, String typeName, 
            boolean isMany, boolean isOrdered, boolean isUnique, EObject op, ModelDictionary dictionary) {
        if (className == null || className.isBlank() || opName == null || opName.isBlank()) return;
        
        String retType = computeCollectionType(typeName, isMany, isOrdered, isUnique);
        dictionary.addTypeMember(className, opName, retType);
        dictionary.addClassElement(className, opName, op);
    }

    protected static void registerGlobalFeature(String pName, String typeName, String ownerName, boolean containment,
            boolean hasSetter, EObject p, ModelDictionary dictionary) {
        if (pName != null && !pName.isBlank()) {
            dictionary.addAutocompleteWord(pName);
            dictionary.addGlobalElement(pName, p);
            String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
            dictionary.addAutocompleteWord("get" + cap);
            dictionary.addGlobalElement("get" + cap, p);
            if (hasSetter) {
                dictionary.addAutocompleteWord("set" + cap);
                dictionary.addGlobalElement("set" + cap, p);
            }
            
            // MDE4CPP only generates create<T>_as_<ref>_in_<Owner> for containment/composite
            // features - the owner must actually contain the created element.
            if (containment && typeName != null && ownerName != null) {
                String factoryMethod = "create" + typeName + "_as_" + pName + "_in_" + ownerName;
                dictionary.addAutocompleteWord(factoryMethod);
                dictionary.addGlobalElement(factoryMethod, p);
                dictionary.addTypeMember(FACTORY_TYPE, factoryMethod, "std::shared_ptr<" + typeName + ">");
            }
        }
    }
    
    protected static void registerGlobalOperation(String opName, EObject op, ModelDictionary dictionary) {
        if (opName != null && !opName.isBlank()) {
            dictionary.addAutocompleteWord(opName);
            dictionary.addGlobalElement(opName, op);
        }
    }

    protected static String computeCollectionType(String typeName, boolean isMany, boolean isOrdered, boolean isUnique) {
        String baseType = typeName != null ? typeName : "Object";
        if (!isMany) return baseType;
        String col = "Bag";
        if (isOrdered && isUnique) col = "OrderedSet";
        else if (isOrdered && !isUnique) col = "Sequence";
        else if (!isOrdered && isUnique) col = "Set";
        return col + "<" + baseType + ">";
    }
}
