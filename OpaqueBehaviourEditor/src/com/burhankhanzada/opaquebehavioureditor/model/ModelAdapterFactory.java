package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.Map;

import org.eclipse.uml2.uml.OpaqueBehavior;

public class ModelAdapterFactory {
    
    public static IModelAdapter createAdapter(Object element) {
        if (element instanceof OpaqueBehavior) {
            return new UmlOpaqueBehaviorAdapter((OpaqueBehavior) element);
        } else if (element instanceof Map.Entry) {
            return new EcoreAnnotationAdapter(element);
        }
        return null;
    }
}
