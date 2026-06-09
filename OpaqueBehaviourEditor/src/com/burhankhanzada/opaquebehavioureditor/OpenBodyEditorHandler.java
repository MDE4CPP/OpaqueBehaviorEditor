package com.burhankhanzada.opaquebehavioureditor;

import java.util.Set;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.burhankhanzada.opaquebehavioureditor.model.IModelAdapter;
import com.burhankhanzada.opaquebehavioureditor.model.ModelAdapterFactory;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.ui.OpaqueBehaviorBodyDialog;

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

        // ---- 2. Create Adapter ----
        IModelAdapter adapter = ModelAdapterFactory.createAdapter(element);
        if (adapter == null) {
            MessageDialog.openWarning(shell, "Body Editor",
                    "Selected element is neither an OpaqueBehavior nor a valid Map Entry.\n"
                  + "Type: " + element.getClass().getName());
            return null;
        }

        // ---- 3. Collect Model Context Types and Completion Words ----
        Set<String> contextTypes = new HashSet<>();
        ModelDictionary dictionary = new ModelDictionary();
        dictionary.autocompleteWords.add("factory");
        
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        ISelectionProvider selectionProvider = 
            (activePart != null && activePart.getSite() != null) ? activePart.getSite().getSelectionProvider() : null;

        adapter.harvestModelContext(contextTypes, dictionary);

        // ---- 4. Open the Editor Dialog ----
        OpaqueBehaviorBodyDialog dialog = new OpaqueBehaviorBodyDialog(
                shell, 
                adapter.getBodies(), 
                adapter.getLanguages(), 
                adapter.getName(), 
                contextTypes, 
                dictionary, 
                selectionProvider, 
                adapter.isUml()
        );

        Runnable saveAction = () -> {
            // ---- 5. Apply Changes via EMF Command Framework ----
            adapter.applyChanges(dialog.getBodies(), dialog.getLanguages(), activePart);

            // ---- 6. Update Eclipse IMarkers for validation errors ----
            adapter.updateMarkers(dialog.getBodies(), dialog.getLanguages(), dictionary);
        };

        dialog.setSaveAction(saveAction);

        if (dialog.open() == Window.OK) {
            saveAction.run();
        }

        return null;
    }
}
