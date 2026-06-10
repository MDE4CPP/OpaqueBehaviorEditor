package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Collections;

import org.eclipse.emf.ecore.EObject;

/**
 * A central dictionary that stores extracted elements from the UML or Ecore model
 * so they can be referenced quickly for auto-completion and hyperlink navigation.
 */
public class ModelDictionary {
    private final TreeSet<String> autocompleteWords = new TreeSet<>();
    private final Map<String, Map<String, EObject>> classElements = new HashMap<>();
    private final Map<String, EObject> globalElements = new HashMap<>();
    private final Map<String, Map<String, String>> typeMembers = new HashMap<>();

    public java.util.Set<String> getAutocompleteWords() { return Collections.unmodifiableSet(autocompleteWords); }
    public Map<String, Map<String, EObject>> getClassElements() { return Collections.unmodifiableMap(classElements); }
    public Map<String, EObject> getGlobalElements() { return Collections.unmodifiableMap(globalElements); }
    public Map<String, Map<String, String>> getTypeMembers() { return Collections.unmodifiableMap(typeMembers); }

    public void addAutocompleteWord(String word) { autocompleteWords.add(word); }
    
    public void addGlobalElement(String name, EObject obj) { globalElements.put(name, obj); }

    public void addClassElement(String className, String memberName, EObject obj) {
        classElements.computeIfAbsent(className, k -> new HashMap<>()).put(memberName, obj);
    }

    public void addTypeMember(String className, String memberName, String returnType) {
        typeMembers.computeIfAbsent(className, k -> new HashMap<>()).put(memberName, returnType);
    }

    public void addTypeMemberMap(String className) {
        typeMembers.putIfAbsent(className, new HashMap<>());
    }
}
