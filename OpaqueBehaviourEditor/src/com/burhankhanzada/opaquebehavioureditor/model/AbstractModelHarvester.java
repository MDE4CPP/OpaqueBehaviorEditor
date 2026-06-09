package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.emf.ecore.EObject;

/**
 * Shared base class containing common logic for harvesting UML/Ecore models.
 */
public abstract class AbstractModelHarvester {

    protected static void registerType(String typeName, EObject obj, Set<String> contextTypes, ModelDictionary dictionary) {
        if (typeName != null && !typeName.isBlank()) {
            contextTypes.add(typeName);
            dictionary.autocompleteWords.add(typeName);
            dictionary.globalElements.put(typeName, obj);
        }
    }

    protected static void registerClassCreate(String className, EObject obj, ModelDictionary dictionary) {
        if (className != null && !className.isBlank()) {
            dictionary.autocompleteWords.add("create" + className);
            dictionary.globalElements.put("create" + className, obj);
        }
    }

    protected static void registerPropertyMember(String className, String pName, String typeName, 
            boolean isMany, boolean isOrdered, boolean isUnique, EObject p, ModelDictionary dictionary) {
        if (className == null || className.isBlank() || pName == null || pName.isBlank()) return;
        
        Map<String, String> members = dictionary.typeMembers.computeIfAbsent(className, k -> new HashMap<>());
        Map<String, EObject> elemMembers = dictionary.classElements.computeIfAbsent(className, k -> new HashMap<>());
        
        String retType = computeCollectionType(typeName, isMany, isOrdered, isUnique);
        
        members.put(pName, retType);
        elemMembers.put(pName, p);
        String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
        members.put("get" + cap, retType);
        elemMembers.put("get" + cap, p);
        members.put("set" + cap, "void");
        elemMembers.put("set" + cap, p);
    }
    
    protected static void registerOperationMember(String className, String opName, String typeName, 
            boolean isMany, boolean isOrdered, boolean isUnique, EObject op, ModelDictionary dictionary) {
        if (className == null || className.isBlank() || opName == null || opName.isBlank()) return;
        
        Map<String, String> members = dictionary.typeMembers.computeIfAbsent(className, k -> new HashMap<>());
        Map<String, EObject> elemMembers = dictionary.classElements.computeIfAbsent(className, k -> new HashMap<>());
        
        String retType = computeCollectionType(typeName, isMany, isOrdered, isUnique);
        members.put(opName, retType);
        elemMembers.put(opName, op);
    }

    protected static void registerGlobalFeature(String pName, String typeName, String ownerName, EObject p, ModelDictionary dictionary) {
        if (pName != null && !pName.isBlank()) {
            dictionary.autocompleteWords.add(pName);
            dictionary.globalElements.put(pName, p);
            String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
            dictionary.autocompleteWords.add("get" + cap);
            dictionary.globalElements.put("get" + cap, p);
            dictionary.autocompleteWords.add("set" + cap);
            dictionary.globalElements.put("set" + cap, p);
            
            if (typeName != null && ownerName != null) {
                dictionary.autocompleteWords.add("create" + typeName + "_as_" + pName + "_in_" + ownerName);
                dictionary.globalElements.put("create" + typeName + "_as_" + pName + "_in_" + ownerName, p);
            }
        }
    }
    
    protected static void registerGlobalOperation(String opName, EObject op, ModelDictionary dictionary) {
        if (opName != null && !opName.isBlank()) {
            dictionary.autocompleteWords.add(opName);
            dictionary.globalElements.put(opName, op);
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
