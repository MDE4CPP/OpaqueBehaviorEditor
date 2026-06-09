package com.burhankhanzada.opaquebehavioureditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.ChangeCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.uml2.uml.OpaqueBehavior;

import com.burhankhanzada.opaquebehavioureditor.markers.MarkerManager;

public class UmlOpaqueBehaviorAdapter implements IModelAdapter {

    private final OpaqueBehavior behavior;

    public UmlOpaqueBehaviorAdapter(OpaqueBehavior behavior) {
        this.behavior = behavior;
    }

    @Override
    public List<String> getBodies() {
        return new ArrayList<>(behavior.getBodies());
    }

    @Override
    public List<String> getLanguages() {
        return new ArrayList<>(behavior.getLanguages());
    }

    @Override
    public String getName() {
        return behavior.getName();
    }

    @Override
    public boolean isUml() {
        return true;
    }

    @Override
    public void harvestModelContext(Set<String> contextTypes, ModelDictionary dictionary) {
        if (behavior.getModel() != null) {
            UmlModelHarvester.harvest(behavior, contextTypes, dictionary);
        }
    }

    @Override
    public void applyChanges(List<String> bodies, List<String> languages, IWorkbenchPart activePart) {
        EditingDomain domain = null;
        if (activePart instanceof org.eclipse.emf.edit.domain.IEditingDomainProvider) {
            domain = ((org.eclipse.emf.edit.domain.IEditingDomainProvider) activePart).getEditingDomain();
        }
        if (domain == null) {
            domain = AdapterFactoryEditingDomain.getEditingDomainFor(behavior);
            EObject parent = behavior;
            while (domain == null && parent.eContainer() != null) {
                parent = parent.eContainer();
                domain = AdapterFactoryEditingDomain.getEditingDomainFor(parent);
            }
        }

        if (domain != null) {
            ChangeCommand cmd = new ChangeCommand(behavior) {
                @Override
                protected void doExecute() {
                    behavior.getBodies().clear();
                    behavior.getBodies().addAll(bodies);
                    behavior.getLanguages().clear();
                    behavior.getLanguages().addAll(languages);
                }
            };
            domain.getCommandStack().execute(cmd);
        } else {
            behavior.getBodies().clear();
            behavior.getBodies().addAll(bodies);
            behavior.getLanguages().clear();
            behavior.getLanguages().addAll(languages);
        }
    }

    @Override
    public void updateMarkers(List<String> bodies, List<String> languages, ModelDictionary dictionary) {
        MarkerManager.updateMarkers(behavior, bodies, languages, dictionary);
    }
}
