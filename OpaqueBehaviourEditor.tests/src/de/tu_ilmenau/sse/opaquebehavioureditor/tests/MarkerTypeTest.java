package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.Activator;
import de.tu_ilmenau.sse.opaquebehavioureditor.markers.MarkerManager;

/**
 * A marker only reaches the Problems view if the type the code creates is the one plugin.xml
 * declares, and if that type derives from org.eclipse.core.resources.problemmarker. Neither is
 * checked by the compiler, and createMarker does not fail on an unknown type - it just produces a
 * marker nothing displays.
 */
public class MarkerTypeTest {

    @Test
    public void markerTypeIsDeclared() {
        IExtension extension = Platform.getExtensionRegistry()
                .getExtension("org.eclipse.core.resources.markers", MarkerManager.MARKER_ID);
        assertNotNull("No marker type declared as " + MarkerManager.MARKER_ID
                + " - markers created with it never appear in the Problems view", extension);
    }

    @Test
    public void pluginIdMatchesTheBundleSymbolicName() {
        assertNotNull(Platform.getBundle(Activator.PLUGIN_ID));
    }

    @Test
    public void markerTypeIsAProblemMarker() {
        assertTrue("The Problems view only shows subtypes of problemmarker",
                org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
                        .getRoot().getWorkspace() != null);
        IExtension extension = Platform.getExtensionRegistry()
                .getExtension("org.eclipse.core.resources.markers", MarkerManager.MARKER_ID);
        assertNotNull(extension);
        boolean isProblem = false;
        for (org.eclipse.core.runtime.IConfigurationElement c : extension.getConfigurationElements()) {
            if ("super".equals(c.getName())
                    && "org.eclipse.core.resources.problemmarker".equals(c.getAttribute("type"))) {
                isProblem = true;
            }
        }
        assertTrue("marker type must extend org.eclipse.core.resources.problemmarker", isProblem);
    }
}
