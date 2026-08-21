package de.tu_ilmenau.sse.opaquebehavioureditor.markers;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.EObject;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageDef;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageMapping;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.TextRange;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelValidator;
import de.tu_ilmenau.sse.opaquebehavioureditor.utils.PluginLogger;

public class MarkerManager {

    public static final String MARKER_ID = "de.tu_ilmenau.sse.opaquebehavioureditor.problem";

    /**
     * @param element the UML behaviour or the Ecore element owning the annotation being edited;
     *                markers are attached to the file that holds it.
     */
    public static void updateMarkers(EObject element, List<String> bodies, List<String> languages, ModelDictionary dictionary) {
        if (element == null) return;
        IFile file = getWorkspaceFile(element);
        if (file == null) return;

        try {
            // Delete existing markers for this specific element
            String behaviorURI = EcoreUtil.getURI(element).toString();
            IMarker[] existingMarkers = file.findMarkers(MARKER_ID, true, IResource.DEPTH_ZERO);
            for (IMarker m : existingMarkers) {
                if (behaviorURI.equals(m.getAttribute(EValidator.URI_ATTRIBUTE))) {
                    m.delete();
                }
            }

            // Run validation and create new markers
            ModelValidator validator = new ModelValidator(dictionary);
            for (int i = 0; i < bodies.size(); i++) {
                String body = bodies.get(i);
                String lang = i < languages.size() ? languages.get(i) : "";
                LanguageDef langDef = LanguageMapping.getLanguageDef(lang);
                
                List<TextRange> errors = validator.validateMemberAccess(body, langDef);
                errors.addAll(validator.validateStatements(body, langDef));
                errors.addAll(validator.validateSyntax(body, langDef));
                for (TextRange error : errors) {
                    createMarker(file, behaviorURI, body, error, lang);
                }
            }
        } catch (CoreException e) {
            PluginLogger.logError("Failed to update model validation markers", e);
        }
    }

    private static void createMarker(IFile file, String behaviorURI, String body, TextRange error, String lang) throws CoreException {
        IMarker marker = file.createMarker(MARKER_ID);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

        // The validator already phrased the problem; the body offset means nothing to a reader of
        // the Problems view, so say where in the body it is instead.
        int line = lineOf(body, error.offset);
        marker.setAttribute(IMarker.MESSAGE, error.message + "  (" + lang + " body, line " + line + ")");
        marker.setAttribute(IMarker.LOCATION, "line " + line);
        marker.setAttribute(IMarker.LINE_NUMBER, line);

        // Link to the EMF element so the model editor can navigate to it
        marker.setAttribute(EValidator.URI_ATTRIBUTE, behaviorURI);
    }

    /** 1-based line of {@code offset} within the body. */
    private static int lineOf(String body, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < body.length(); i++) {
            if (body.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static IFile getWorkspaceFile(EObject element) {
        Resource eResource = element.eResource();
        if (eResource != null && eResource.getURI().isPlatformResource()) {
            String path = eResource.getURI().toPlatformString(true);
            return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
        }
        return null;
    }
}
