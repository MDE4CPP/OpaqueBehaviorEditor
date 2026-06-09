import org.eclipse.core.resources.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.common.util.URI;
public class TestIMarker {
    public static IFile getFile(Resource res) {
        URI uri = res.getURI();
        if (uri.isPlatformResource()) {
            String path = uri.toPlatformString(true);
            return ResourcesPlugin.getWorkspace().getRoot().getFile(new org.eclipse.core.runtime.Path(path));
        }
        return null;
    }
}
