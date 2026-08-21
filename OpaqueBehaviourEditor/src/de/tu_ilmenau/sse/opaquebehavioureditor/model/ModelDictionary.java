package de.tu_ilmenau.sse.opaquebehavioureditor.model;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
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
    private final Map<String, Set<String>> superTypes = new HashMap<>();
    private final Map<String, String> contextVariables = new HashMap<>();

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

    /**
     * Registers a name that is in scope for the body without being declared in it - the injected
     * factory, or the parameters of the operation the body implements.
     */
    public void addContextVariable(String name, String typeName) {
        if (name != null && !name.isBlank() && typeName != null && !typeName.isBlank()) {
            contextVariables.put(name, typeName);
        }
    }

    public String getContextVariableType(String name) {
        return name == null ? null : contextVariables.get(name);
    }

    public void addTypeMemberMap(String className) {
        typeMembers.putIfAbsent(className, new HashMap<>());
    }

    /**
     * Registers that {@code className} has {@code superTypeName} as a direct supertype.
     * This is used to resolve inherited members transitively.
     */
    public void addSuperType(String className, String superTypeName) {
        if (className != null && !className.isBlank() && superTypeName != null && !superTypeName.isBlank()) {
            superTypes.computeIfAbsent(className, k -> new HashSet<>()).add(superTypeName);
        }
    }

    /**
     * Returns all type members for the given className, including members
     * inherited transitively from all supertypes. If the className is not
     * known, returns an empty map.
     */
    public Map<String, String> getAllTypeMembers(String className) {
        Map<String, String> result = new HashMap<>();
        collectAllTypeMembers(className, result, new HashSet<>());
        return result;
    }

    /**
     * Returns the type that declares {@code member} for {@code className} - the class itself if it
     * declares it, otherwise the nearest supertype that does, or {@code null} if no type in the
     * hierarchy has it. Lets callers tell a class's own members apart from inherited and
     * metamodel ones.
     */
    public String getDeclaringType(String className, String member) {
        if (className == null || member == null) return null;
        Set<String> visited = new HashSet<>();
        java.util.Deque<String> queue = new java.util.ArrayDeque<>();
        queue.add(className);
        while (!queue.isEmpty()) {
            String type = queue.poll();
            if (!visited.add(type)) continue;
            Map<String, String> members = typeMembers.get(type);
            if (members != null && members.containsKey(member)) return type;
            Set<String> supers = superTypes.get(type);
            if (supers != null) queue.addAll(supers);
        }
        return null;
    }

    /**
     * Recursively collects type members from the given className and all its
     * supertypes. Uses a visited set to prevent infinite loops from cycles.
     */
    private void collectAllTypeMembers(String className, Map<String, String> result, Set<String> visited) {
        if (className == null || !visited.add(className)) {
            return;
        }
        // Add direct members of this type
        Map<String, String> directMembers = typeMembers.get(className);
        if (directMembers != null) {
            for (Map.Entry<String, String> entry : directMembers.entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        // Recurse into supertypes
        Set<String> supers = superTypes.get(className);
        if (supers != null) {
            for (String superType : supers) {
                collectAllTypeMembers(superType, result, visited);
            }
        }
    }
}

