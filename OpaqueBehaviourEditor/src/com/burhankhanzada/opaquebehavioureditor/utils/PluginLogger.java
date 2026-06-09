package com.burhankhanzada.opaquebehavioureditor.utils;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import com.burhankhanzada.opaquebehavioureditor.Activator;

public class PluginLogger {

    private PluginLogger() {
        // Utility class
    }

    public static void logError(String message, Throwable t) {
        if (Activator.getDefault() == null) {
            System.err.println("ERROR: " + message);
            if (t != null) t.printStackTrace();
            return;
        }
        
        ILog log = Activator.getDefault().getLog();
        log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, t));
    }

    public static void logWarning(String message) {
        if (Activator.getDefault() == null) {
            System.out.println("WARN: " + message);
            return;
        }
        
        ILog log = Activator.getDefault().getLog();
        log.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID, message, null));
    }
    
    public static void logInfo(String message) {
        if (Activator.getDefault() == null) {
            System.out.println("INFO: " + message);
            return;
        }
        
        ILog log = Activator.getDefault().getLog();
        log.log(new Status(IStatus.INFO, Activator.PLUGIN_ID, message, null));
    }
}
