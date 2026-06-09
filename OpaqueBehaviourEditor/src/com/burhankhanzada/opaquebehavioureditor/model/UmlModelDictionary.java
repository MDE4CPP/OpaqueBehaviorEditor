package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.Map;
import java.util.TreeSet;

import org.eclipse.emf.ecore.EObject;

/**
 * A central dictionary that stores extracted elements from the UML model
 * so they can be referenced quickly for auto-completion and hyperlink navigation.
 */
public class UmlModelDictionary {
    /** General words that appear in the autocomplete popup (e.g., class names, global properties). */
    public final TreeSet<String> autocompleteWords = new TreeSet<>();
    
    /** Maps a class name to its members (properties/operations) mapped to their corresponding UML EObjects. */
    public final Map<String, Map<String, EObject>> classElements = new java.util.HashMap<>();
    
    /** Maps a globally available word (like a factory method or a root property) to its UML EObject. */
    public final Map<String, EObject> globalElements = new java.util.HashMap<>();
    
    /** Maps a class name to its members mapped to their string return types (used for autocomplete context resolution). */
    public final Map<String, Map<String, String>> typeMembers = new java.util.HashMap<>();
}
