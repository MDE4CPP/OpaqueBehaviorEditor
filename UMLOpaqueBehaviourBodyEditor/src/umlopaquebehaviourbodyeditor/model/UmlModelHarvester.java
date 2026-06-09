package umlopaquebehaviourbodyeditor.model;

import umlopaquebehaviourbodyeditor.ui.*;
import umlopaquebehaviourbodyeditor.editor.*;
import umlopaquebehaviourbodyeditor.model.*;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.OpaqueBehavior;

/**
 * Utility class to traverse a UML Model and harvest elements (Classes, Properties, Operations)
 * to populate the dictionaries used for auto-completion and syntax highlighting.
 */
public class UmlModelHarvester {

    /**
     * Traverses the model associated with the given behavior and populates the provided
     * contextTypes set and UmlModelDictionary.
     *
     * @param behavior     The OpaqueBehavior whose model will be traversed.
     * @param contextTypes A Set to populate with discovered type names.
     * @param dictionary   The dictionary to populate with autocomplete words and elements.
     */
    public static void harvest(OpaqueBehavior behavior, Set<String> contextTypes, UmlModelDictionary dictionary) {
        if (behavior.getModel() == null) {
            return;
        }

        TreeIterator<EObject> it = behavior.getModel().eAllContents();
        while (it.hasNext()) {
            EObject obj = it.next();
            
            // 1. Handle general UML Types (Classes, Interfaces, PrimitiveTypes, etc.)
            if (obj instanceof org.eclipse.uml2.uml.Type t) {
                String typeName = t.getName();
                if (typeName != null && !typeName.isBlank()) {
                    contextTypes.add(typeName);
                    dictionary.autocompleteWords.add(typeName);
                    dictionary.globalElements.put(typeName, t);
                }
            }
            
            // 2. Handle UML Classes specifically to add MDE4CPP factory "create" methods
            if (obj instanceof org.eclipse.uml2.uml.Class c) {
                String className = c.getName();
                if (className != null && !className.isBlank()) {
                    dictionary.autocompleteWords.add("create" + className);
                    dictionary.globalElements.put("create" + className, c);
                }
            }
            
            // 3. Handle Classifiers to extract their properties and operations for member-access completion
            if (obj instanceof org.eclipse.uml2.uml.Classifier classifier) {
                String className = classifier.getName();
                if (className != null && !className.isBlank()) {
                    Map<String, String> members = dictionary.typeMembers.computeIfAbsent(className, k -> new HashMap<>());
                    Map<String, EObject> elemMembers = dictionary.classElements.computeIfAbsent(className, k -> new HashMap<>());
                    
                    // Register attributes (properties) as members, including their generated getters/setters
                    for (org.eclipse.uml2.uml.Property p : classifier.getAllAttributes()) {
                        String pName = p.getName();
                        if (pName != null && !pName.isBlank()) {
                            String typeName = p.getType() != null ? p.getType().getName() : "Object";
                            String retType = typeName;
                            if (p.isMultivalued()) {
                                String col = "Bag";
                                if (p.isOrdered() && p.isUnique()) col = "OrderedSet";
                                else if (p.isOrdered() && !p.isUnique()) col = "Sequence";
                                else if (!p.isOrdered() && p.isUnique()) col = "Set";
                                retType = col + "<" + typeName + ">";
                            }
                            members.put(pName, retType);
                            elemMembers.put(pName, p);
                            String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                            members.put("get" + cap, retType);
                            elemMembers.put("get" + cap, p);
                            members.put("set" + cap, "void");
                            elemMembers.put("set" + cap, p);
                        }
                    }
                    
                    // Register operations (methods) as members
                    for (org.eclipse.uml2.uml.Operation op : classifier.getAllOperations()) {
                        String opName = op.getName();
                        if (opName != null && !opName.isBlank()) {
                            String typeName = op.getType() != null ? op.getType().getName() : "void";
                            String retType = typeName;
                            org.eclipse.uml2.uml.Parameter retParam = op.getReturnResult();
                            if (retParam != null && retParam.isMultivalued()) {
                                String col = "Bag";
                                if (retParam.isOrdered() && retParam.isUnique()) col = "OrderedSet";
                                else if (retParam.isOrdered() && !retParam.isUnique()) col = "Sequence";
                                else if (!retParam.isOrdered() && retParam.isUnique()) col = "Set";
                                retType = col + "<" + typeName + ">";
                            }
                            members.put(opName, retType);
                            elemMembers.put(opName, op);
                        }
                    }
                }
            }
            
            // 4. Register operations as global words (for standalone function calls or auto-completion fallback)
            if (obj instanceof org.eclipse.uml2.uml.Operation op) {
                String opName = op.getName();
                if (opName != null && !opName.isBlank()) {
                    dictionary.autocompleteWords.add(opName);
                    dictionary.globalElements.put(opName, op);
                }
            }
            
            // 5. Register properties as global words and generate their specific factory 'create_as' methods
            if (obj instanceof org.eclipse.uml2.uml.Property p) {
                String pName = p.getName();
                if (pName != null && !pName.isBlank()) {
                    dictionary.autocompleteWords.add(pName);
                    dictionary.globalElements.put(pName, p);
                    String cap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                    dictionary.autocompleteWords.add("get" + cap);
                    dictionary.globalElements.put("get" + cap, p);
                    dictionary.autocompleteWords.add("set" + cap);
                    dictionary.globalElements.put("set" + cap, p);
                    
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
                    
                    if (typeName != null && ownerName != null) {
                        dictionary.autocompleteWords.add("create" + typeName + "_as_" + pName + "_in_" + ownerName);
                        dictionary.globalElements.put("create" + typeName + "_as_" + pName + "_in_" + ownerName, p);
                    }
                }
            }
        }
    }
}
