package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.ChangeCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.ui.IWorkbenchPart;

import com.burhankhanzada.opaquebehavioureditor.StringConstants;

public class EcoreAnnotationAdapter implements IModelAdapter {

    private final Map.Entry<String, String> mapEntry;
    private final EObject emfElement;

    @SuppressWarnings("unchecked")
    public EcoreAnnotationAdapter(Object element) {
        this.mapEntry = (Map.Entry<String, String>) element;
        this.emfElement = (element instanceof EObject) ? (EObject) element : null;
    }

    @Override
    public List<String> getBodies() {
        List<String> bodies = new ArrayList<>();
        String val = mapEntry.getValue();
        bodies.add(val != null ? val : "");
        return bodies;
    }

    @Override
    public List<String> getLanguages() {
        List<String> languages = new ArrayList<>();
        languages.add("C++"); // Default language for Ecore annotations
        return languages;
    }

    @Override
    public String getName() {
        return String.format(StringConstants.FORMAT_ANNOTATION_NAME, mapEntry.getKey());
    }

    @Override
    public boolean isUml() {
        return false;
    }

    @Override
    public void harvestModelContext(Set<String> contextTypes, ModelDictionary dictionary) {
        if (emfElement != null) {
            EObject root = org.eclipse.emf.ecore.util.EcoreUtil.getRootContainer(emfElement);
            if (root instanceof org.eclipse.emf.ecore.EPackage) {
                EcoreModelHarvester.harvest((org.eclipse.emf.ecore.EPackage) root, contextTypes, dictionary);
            }
        }
    }

    @Override
    public void applyChanges(List<String> bodies, List<String> languages, IWorkbenchPart activePart) {
        if (bodies.isEmpty()) return;
        String newBody = bodies.get(0);

        if (emfElement != null) {
            EditingDomain domain = null;
            if (activePart instanceof org.eclipse.emf.edit.domain.IEditingDomainProvider) {
                domain = ((org.eclipse.emf.edit.domain.IEditingDomainProvider) activePart).getEditingDomain();
            }
            if (domain == null) {
                domain = AdapterFactoryEditingDomain.getEditingDomainFor(emfElement);
                EObject parent = emfElement;
                while (domain == null && parent.eContainer() != null) {
                    parent = parent.eContainer();
                    domain = AdapterFactoryEditingDomain.getEditingDomainFor(parent);
                }
            }

            if (domain != null) {
                org.eclipse.emf.common.notify.Notifier notifier = emfElement.eResource() != null ? emfElement.eResource() : emfElement;
                ChangeCommand cmd = new ChangeCommand(notifier) {
                    @Override
                    protected void doExecute() {
                        mapEntry.setValue(newBody);
                    }
                };
                domain.getCommandStack().execute(cmd);
                return;
            }
        }
        
        mapEntry.setValue(newBody);
    }

    @Override
    public void updateMarkers(List<String> bodies, List<String> languages, ModelDictionary dictionary) {
        // No-op for Ecore annotations
    }
}
