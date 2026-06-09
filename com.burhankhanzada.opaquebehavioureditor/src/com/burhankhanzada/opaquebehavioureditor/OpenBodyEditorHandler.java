package com.burhankhanzada.opaquebehavioureditor;

import com.burhankhanzada.opaquebehavioureditor.ui.*;
import com.burhankhanzada.opaquebehavioureditor.editor.*;
import com.burhankhanzada.opaquebehavioureditor.model.*;
import com.burhankhanzada.opaquebehavioureditor.markers.MarkerManager;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.uml2.uml.OpaqueBehavior;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import org.eclipse.uml2.uml.UMLPackage;

/**
 * Command handler that opens the {@link OpaqueBehaviorBodyDialog}
 * for the currently selected {@link OpaqueBehavior} element.
 * 
 * This handler is registered in plugin.xml and serves as the entry point
 * when the user clicks "Edit Body with Code Editor..." from the context menu.
 */
public class OpenBodyEditorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);

        try {
            return doExecute(event, shell);
        } catch (Exception e) {
            MessageDialog.openError(shell, "Body Editor Error",
                    "An error occurred:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    private Object doExecute(ExecutionEvent event, Shell shell) {
        // ---- 1. Resolve Selection ----
        // Extract the selected object from the Eclipse UI context
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty()) {
            MessageDialog.openInformation(shell, "Body Editor",
                    "No element selected.");
            return null;
        }

        Object element = structured.getFirstElement();

        // The debug dialog confirmed the type is OpaqueBehaviorImpl
        if (!(element instanceof OpaqueBehavior behavior)) {
            MessageDialog.openWarning(shell, "Body Editor",
                    "Selected element is not an OpaqueBehavior.\n"
                  + "Type: " + element.getClass().getName());
            return null;
        }

        // ---- 2. Extract Existing Data ----
        // Get the current bodies, languages, and name to pre-populate the dialog
        List<String> bodies    = new ArrayList<>(behavior.getBodies());
        List<String> languages = new ArrayList<>(behavior.getLanguages());
        String       name      = behavior.getName();

        // ---- 3. Collect Model Context Types and Completion Words ----
        // We traverse the entire UML model to collect class names, property names, 
        // and operation names. These are used to populate the auto-completion popup 
        // and semantic highlighter dictionaries.
        Set<String> contextTypes = new HashSet<>();
        UmlModelDictionary dictionary = new UmlModelDictionary();
        dictionary.autocompleteWords.add("factory");
        
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        org.eclipse.jface.viewers.ISelectionProvider selectionProvider = 
            (activePart != null && activePart.getSite() != null) ? activePart.getSite().getSelectionProvider() : null;

        if (behavior.getModel() != null) {
            UmlModelHarvester.harvest(behavior, contextTypes, dictionary);
        }

        // ---- 4. Open the Editor Dialog ----
        OpaqueBehaviorBodyDialog dialog =
                new OpaqueBehaviorBodyDialog(shell, bodies, languages, name, contextTypes, dictionary, selectionProvider);

        if (dialog.open() != Window.OK) {
            return null;
        }

        // ---- 5. Apply Changes via EMF Command Framework ----
        List<String> newBodies    = dialog.getBodies();
        List<String> newLanguages = dialog.getLanguages();

        EditingDomain domain = AdapterFactoryEditingDomain.getEditingDomainFor(behavior);
        if (domain != null) {
            org.eclipse.emf.edit.command.ChangeCommand cmd = new org.eclipse.emf.edit.command.ChangeCommand(behavior) {
                @Override
                protected void doExecute() {
                    behavior.getBodies().clear();
                    behavior.getBodies().addAll(newBodies);
                    behavior.getLanguages().clear();
                    behavior.getLanguages().addAll(newLanguages);
                }
            };
            domain.getCommandStack().execute(cmd);
        } else {
            behavior.getBodies().clear();
            behavior.getBodies().addAll(newBodies);
            behavior.getLanguages().clear();
            behavior.getLanguages().addAll(newLanguages);
        }

        // ---- 6. Update Eclipse IMarkers for validation errors ----
        MarkerManager.updateMarkers(behavior, newBodies, newLanguages, dictionary);

        return null;
    }
}
