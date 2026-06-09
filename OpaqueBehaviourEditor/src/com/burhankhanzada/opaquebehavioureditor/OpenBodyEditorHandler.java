package com.burhankhanzada.opaquebehavioureditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.command.ChangeCommand;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.uml2.uml.OpaqueBehavior;

import com.burhankhanzada.opaquebehavioureditor.markers.MarkerManager;
import com.burhankhanzada.opaquebehavioureditor.ui.OpaqueBehaviorBodyDialog;
import com.burhankhanzada.opaquebehavioureditor.model.UmlModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.UmlModelHarvester;

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
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty()) {
            MessageDialog.openInformation(shell, "Body Editor", "No element selected.");
            return null;
        }

        Object element = structured.getFirstElement();

        List<String> bodies    = new ArrayList<>();
        List<String> languages = new ArrayList<>();
        String       name      = "Ecore Annotation Body";
        EObject      emfElement = null;
        boolean      isUml     = false;

        if (element instanceof OpaqueBehavior behavior) {
            bodies.addAll(behavior.getBodies());
            languages.addAll(behavior.getLanguages());
            name = behavior.getName();
            emfElement = behavior;
            isUml = true;
        } else if (element instanceof Map.Entry<?,?> mapEntry) {
            Object key = mapEntry.getKey();
            Object value = mapEntry.getValue();
            if (value == null || value instanceof String) {
                if (value != null) {
                    bodies.add((String) value);
                } else {
                    bodies.add("");
                }
                languages.add("C++");
                emfElement = (element instanceof EObject eObj) ? eObj : null;
                isUml = false;
                name = "Ecore Annotation (" + key + ")";
            } else {
                MessageDialog.openWarning(shell, "Body Editor",
                        "The selected Map Entry value is not a String.\nKey is: " + key);
                return null;
            }
        } else {
            MessageDialog.openWarning(shell, "Body Editor",
                    "Selected element is neither an OpaqueBehavior nor a valid Map Entry.\n"
                  + "Type: " + element.getClass().getName());
            return null;
        }

        // ---- 3. Collect Model Context Types and Completion Words ----
        Set<String> contextTypes = new HashSet<>();
        UmlModelDictionary dictionary = new UmlModelDictionary();
        dictionary.autocompleteWords.add("factory");
        
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        ISelectionProvider selectionProvider = 
            (activePart != null && activePart.getSite() != null) ? activePart.getSite().getSelectionProvider() : null;

        if (isUml) {
            OpaqueBehavior behavior = (OpaqueBehavior) emfElement;
            if (behavior.getModel() != null) {
                UmlModelHarvester.harvest(behavior, contextTypes, dictionary);
            }
        }

        // ---- 4. Open the Editor Dialog ----
        OpaqueBehaviorBodyDialog dialog =
                new OpaqueBehaviorBodyDialog(shell, bodies, languages, name, contextTypes, dictionary, selectionProvider, isUml);

        if (dialog.open() != Window.OK) {
            return null;
        }

        // ---- 5. Apply Changes via EMF Command Framework ----
        List<String> newBodies    = dialog.getBodies();
        List<String> newLanguages = dialog.getLanguages();

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
        final EObject finalEmfElement = emfElement;
        final boolean finalIsUml = isUml;

        if (domain != null) {
            if (finalIsUml) {
                ChangeCommand cmd = new ChangeCommand(finalEmfElement) {
                    @Override
                    protected void doExecute() {
                        OpaqueBehavior behavior = (OpaqueBehavior) finalEmfElement;
                        behavior.getBodies().clear();
                        behavior.getBodies().addAll(newBodies);
                        behavior.getLanguages().clear();
                        behavior.getLanguages().addAll(newLanguages);
                    }
                };
                domain.getCommandStack().execute(cmd);
            } else {
                // It's a Map Entry. Update the value using ChangeCommand on the resource to ensure it records EMap changes.
                org.eclipse.emf.common.notify.Notifier notifier = finalEmfElement.eResource() != null ? finalEmfElement.eResource() : finalEmfElement;
                ChangeCommand cmd = new ChangeCommand(notifier) {
                    @Override
                    protected void doExecute() {
                        @SuppressWarnings("unchecked")
                        Map.Entry<String, String> mapEntry = (Map.Entry<String, String>) finalEmfElement;
                        if (!newBodies.isEmpty()) {
                            mapEntry.setValue(newBodies.get(0));
                        }
                    }
                };
                domain.getCommandStack().execute(cmd);
            }
        } else {
            if (isUml) {
                OpaqueBehavior behavior = (OpaqueBehavior) emfElement;
                behavior.getBodies().clear();
                behavior.getBodies().addAll(newBodies);
                behavior.getLanguages().clear();
                behavior.getLanguages().addAll(newLanguages);
            } else {
                @SuppressWarnings("unchecked")
                Map.Entry<String, String> mapEntry = (Map.Entry<String, String>) emfElement;
                if (!newBodies.isEmpty()) {
                    mapEntry.setValue(newBodies.get(0));
                }
            }
        }

        // ---- 6. Update Eclipse IMarkers for validation errors ----
        if (isUml) {
            MarkerManager.updateMarkers((OpaqueBehavior) emfElement, newBodies, newLanguages, dictionary);
        }

        return null;
    }
}
