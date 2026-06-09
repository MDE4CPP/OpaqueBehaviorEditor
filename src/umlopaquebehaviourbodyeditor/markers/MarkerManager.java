package umlopaquebehaviourbodyeditor.markers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.OpaqueBehavior;
import umlopaquebehaviourbodyeditor.editor.LanguageMapping;
import umlopaquebehaviourbodyeditor.model.TextRange;
import umlopaquebehaviourbodyeditor.model.UmlModelDictionary;
import umlopaquebehaviourbodyeditor.model.UmlModelValidator;

import java.util.List;

public class MarkerManager {

    public static final String MARKER_ID = "umlopaquebehaviourbodyeditor.problem";

    public static void updateMarkers(OpaqueBehavior behavior, List<String> bodies, List<String> languages, UmlModelDictionary dictionary) {
        IFile file = getWorkspaceFile(behavior);
        if (file == null) return;

        try {
            // Delete existing markers for this specific behavior
            String behaviorURI = EcoreUtil.getURI(behavior).toString();
            IMarker[] existingMarkers = file.findMarkers(MARKER_ID, true, IResource.DEPTH_ZERO);
            for (IMarker m : existingMarkers) {
                if (behaviorURI.equals(m.getAttribute(org.eclipse.emf.ecore.EValidator.URI_ATTRIBUTE))) {
                    m.delete();
                }
            }

            // Run validation and create new markers
            UmlModelValidator validator = new UmlModelValidator(dictionary);
            for (int i = 0; i < bodies.size(); i++) {
                String body = bodies.get(i);
                String lang = i < languages.size() ? languages.get(i) : "";
                LanguageMapping.LanguageDef langDef = LanguageMapping.getLanguageDef(lang);
                
                List<TextRange> errors = validator.validateUMLMemberAccess(body, langDef);
                for (TextRange error : errors) {
                    createMarker(file, behaviorURI, body, error, lang);
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    private static void createMarker(IFile file, String behaviorURI, String body, TextRange error, String lang) throws CoreException {
        IMarker marker = file.createMarker(MARKER_ID);
        marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        
        // Extract the problematic text for the error message
        String errorText = body.substring(error.offset, error.offset + error.length);
        marker.setAttribute(IMarker.MESSAGE, "UML Resolution Error: Unrecognized member '" + errorText + "'");
        
        // Character bounds for the problem view highlight
        marker.setAttribute(IMarker.CHAR_START, error.offset);
        marker.setAttribute(IMarker.CHAR_END, error.offset + error.length);
        
        // Link to the EMF element so Papyrus can navigate to it
        marker.setAttribute(org.eclipse.emf.ecore.EValidator.URI_ATTRIBUTE, behaviorURI);
    }

    private static IFile getWorkspaceFile(OpaqueBehavior behavior) {
        Resource eResource = behavior.eResource();
        if (eResource != null && eResource.getURI().isPlatformResource()) {
            String path = eResource.getURI().toPlatformString(true);
            return ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path));
        }
        return null;
    }
}
