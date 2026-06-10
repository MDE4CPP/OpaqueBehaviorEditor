package com.burhankhanzada.opaquebehavioureditor.model;

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

    protected static void registerClassCreate(String className, EObject obj, ModelDictionary dictionary) {
        if (className != null && !className.isBlank()) {
            dictionary.addAutocompleteWord("create" + className);
            dictionary.addGlobalElement("create" + className, obj);
        }
    }

    protected static void registerPropertyMember(String className, String pName, String typeName, 
            boolean isMany, boolean isOrdered, boolean isUnique, EObject p, ModelDictionary dictionary) {
        if (className == null || className.isBlank() || pName == null || pName.isBlank()) return;
        
        String retType = computeCollectionType(typeName, isMany, isOrdered, isUnique);
        
        dictionary.addTypeMember(className, pName, retType);
        dictionary.addClassElement(className, pName, p);
        String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
        dictionary.addTypeMember(className, "get" + cap, retType);
        dictionary.addClassElement(className, "get" + cap, p);
        dictionary.addTypeMember(className, "set" + cap, "void");
        dictionary.addClassElement(className, "set" + cap, p);
    }
    
    protected static void registerOperationMember(String className, String opName, String typeName, 
            boolean isMany, boolean isOrdered, boolean isUnique, EObject op, ModelDictionary dictionary) {
        if (className == null || className.isBlank() || opName == null || opName.isBlank()) return;
        
        String retType = computeCollectionType(typeName, isMany, isOrdered, isUnique);
        dictionary.addTypeMember(className, opName, retType);
        dictionary.addClassElement(className, opName, op);
    }

    protected static void registerGlobalFeature(String pName, String typeName, String ownerName, EObject p, ModelDictionary dictionary) {
        if (pName != null && !pName.isBlank()) {
            dictionary.addAutocompleteWord(pName);
            dictionary.addGlobalElement(pName, p);
            String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
            dictionary.addAutocompleteWord("get" + cap);
            dictionary.addGlobalElement("get" + cap, p);
            dictionary.addAutocompleteWord("set" + cap);
            dictionary.addGlobalElement("set" + cap, p);
            
            if (typeName != null && ownerName != null) {
                dictionary.addAutocompleteWord("create" + typeName + "_as_" + pName + "_in_" + ownerName);
                dictionary.addGlobalElement("create" + typeName + "_as_" + pName + "_in_" + ownerName, p);
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
