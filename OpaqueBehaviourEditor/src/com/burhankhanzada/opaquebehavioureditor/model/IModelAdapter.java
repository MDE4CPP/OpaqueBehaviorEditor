package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.List;
import java.util.Set;

import org.eclipse.ui.IWorkbenchPart;

/**
 * Encapsulates EMF reading/writing logic for different model elements 
 * (e.g. UML OpaqueBehavior vs Ecore Map.Entry).
 */
public interface IModelAdapter {
    
    /** Gets the initial list of bodies to edit. */
    List<String> getBodies();
    
    /** Gets the initial list of languages corresponding to the bodies. */
    List<String> getLanguages();
    
    /** Gets the display name for the dialog title/message. */
    String getName();
    
    /** Returns true if this is a UML element (controls multi-body UI features). */
    boolean isUml();
    
    /** 
     * Extracts autocomplete context and populates the dictionary based on the underlying model.
     */
    void harvestModelContext(Set<String> contextTypes, ModelDictionary dictionary);
    
    /** 
     * Applies the modified bodies and languages back to the underlying EMF model
     * using the appropriate EditingDomain and ChangeCommand.
     */
    void applyChanges(List<String> bodies, List<String> languages, IWorkbenchPart activePart);
    
    /** 
     * Updates Eclipse IMarkers for validation errors (if supported by the model type).
     */
    void updateMarkers(List<String> bodies, List<String> languages, ModelDictionary dictionary);
}
