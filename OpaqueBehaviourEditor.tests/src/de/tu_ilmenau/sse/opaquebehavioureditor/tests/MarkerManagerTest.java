package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.markers.MarkerManager;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.EcoreModelHarvester;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;

/**
 * Drives the whole marker path against a real workspace file: a marker that is created but not
 * displayed looks exactly like one that was never created, so only this catches it.
 */
public class MarkerManagerTest {

    private static final String MODEL =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      + "<ecore:EPackage xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\""
      + " xmlns:ecore=\"http://www.eclipse.org/emf/2002/Ecore\" name=\"lib\""
      + " nsURI=\"http://example.org/lib\" nsPrefix=\"lib\">"
      + "  <eAnnotations source=\"http://tu-ilmenau.de/see/prepareApplication\">"
      + "    <details key=\"body\" value=\"jhnjknkjn;\"/>"
      + "  </eAnnotations>"
      + "  <eClassifiers xsi:type=\"ecore:EClass\" name=\"Book\""
      + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>"
      + "</ecore:EPackage>";

    private static IFile writeModel(String projectName) throws Exception {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) project.create(null);
        project.open(null);
        IFile file = project.getFile("lib.ecore");
        ByteArrayInputStream in = new ByteArrayInputStream(MODEL.getBytes(StandardCharsets.UTF_8));
        if (file.exists()) file.setContents(in, true, false, null);
        else file.create(in, true, null);
        return file;
    }

    @Test
    public void aBodyProblemBecomesAMarkerOnTheModelFile() throws Exception {
        IFile file = writeModel("markerTestProject");

        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        Resource r = rs.getResource(
                URI.createPlatformResourceURI(file.getFullPath().toString(), true), true);
        EPackage pkg = (EPackage) r.getContents().get(0);
        EAnnotation body = pkg.getEAnnotations().get(0);

        ModelDictionary d = new ModelDictionary();
        EcoreModelHarvester.harvest(pkg, new HashSet<>(), d);

        MarkerManager.updateMarkers(pkg, List.of(body.getDetails().get("body")), List.of("C++"), d);

        IMarker[] markers = file.findMarkers(MarkerManager.MARKER_ID, true, IResource.DEPTH_ZERO);
        assertEquals("expected exactly one marker for the undeclared identifier", 1, markers.length);

        String message = (String) markers[0].getAttribute(IMarker.MESSAGE);
        assertNotNull(message);
        assertTrue("message should name the identifier, was: " + message,
                message.contains("jhnjknkjn"));
        assertEquals(IMarker.SEVERITY_ERROR, markers[0].getAttribute(IMarker.SEVERITY));

        // the Problems view lists it only if it is a problemmarker subtype
        assertTrue(markers[0].isSubtypeOf("org.eclipse.core.resources.problemmarker"));
    }

    @Test
    public void rerunningReplacesMarkersInsteadOfPilingThemUp() throws Exception {
        IFile file = writeModel("markerTestProject2");

        ResourceSet rs = new ResourceSetImpl();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        rs.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);
        Resource r = rs.getResource(
                URI.createPlatformResourceURI(file.getFullPath().toString(), true), true);
        EPackage pkg = (EPackage) r.getContents().get(0);

        ModelDictionary d = new ModelDictionary();
        EcoreModelHarvester.harvest(pkg, new HashSet<>(), d);

        MarkerManager.updateMarkers(pkg, List.of("jhnjknkjn;"), List.of("C++"), d);
        MarkerManager.updateMarkers(pkg, List.of("jhnjknkjn;"), List.of("C++"), d);
        assertEquals(1, file.findMarkers(MarkerManager.MARKER_ID, true, IResource.DEPTH_ZERO).length);

        MarkerManager.updateMarkers(pkg, List.of("// all good\n"), List.of("C++"), d);
        assertEquals(0, file.findMarkers(MarkerManager.MARKER_ID, true, IResource.DEPTH_ZERO).length);
    }
}
