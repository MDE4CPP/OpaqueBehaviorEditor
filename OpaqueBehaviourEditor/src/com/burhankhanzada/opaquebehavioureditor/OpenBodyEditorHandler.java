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
            MessageDialog.openError(shell, StringConstants.POPUP_TITLE_ERR,
                    StringConstants.POPUP_MSG_ERR_PREFIX + e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    private Object doExecute(ExecutionEvent event, Shell shell) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty()) {
            MessageDialog.openInformation(shell, StringConstants.POPUP_TITLE_INFO, StringConstants.POPUP_MSG_NO_SELECTION);
            return null;
        }

        Object element = structured.getFirstElement();

        IModelAdapter adapter = ModelAdapterFactory.createAdapter(element);
        if (adapter == null) {
            MessageDialog.openWarning(shell, StringConstants.POPUP_TITLE_INFO,
                    StringConstants.POPUP_MSG_INVALID_SELECTION + element.getClass().getName());
            return null;
        }

        Set<String> contextTypes = new HashSet<>();
        ModelDictionary dictionary = new ModelDictionary();
        dictionary.addAutocompleteWord("factory");
        dictionary.addGlobalElement("factory", ((org.eclipse.emf.ecore.EObject) element).eContainer());
        
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        ISelectionProvider selectionProvider = 
            (activePart != null && activePart.getSite() != null) ? activePart.getSite().getSelectionProvider() : null;

        adapter.harvestModelContext(contextTypes, dictionary);

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
            adapter.applyChanges(dialog.getBodies(), dialog.getLanguages(), activePart);
            adapter.updateMarkers(dialog.getBodies(), dialog.getLanguages(), dictionary);
        };

        dialog.setSaveAction(saveAction);

        if (dialog.open() == Window.OK) {
            saveAction.run();
        }

        return null;
    }
}
