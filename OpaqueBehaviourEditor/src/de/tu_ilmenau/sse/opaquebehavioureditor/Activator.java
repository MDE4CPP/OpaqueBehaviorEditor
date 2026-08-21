package de.tu_ilmenau.sse.opaquebehavioureditor;

import org.osgi.framework.BundleContext;

import org.eclipse.ui.plugin.AbstractUIPlugin;

public class Activator extends AbstractUIPlugin {

	/** Must match Bundle-SymbolicName in META-INF/MANIFEST.MF, which is hyphenated. */
	public static final String PLUGIN_ID = "de.tu-ilmenau.sse.opaquebehavioureditor";

	private static Activator plugin;
  
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	public static Activator getDefault() {
		return plugin;
	}

}
