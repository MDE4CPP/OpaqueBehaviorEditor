package com.burhankhanzada.opaquebehavioureditor.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.internal.cocoa.*;

public class ThemeUtils {
    
    public static Button createPushButton(Composite parent, String label) {
        Button btn = new Button(parent, SWT.PUSH);
        btn.setText(label);
        btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return btn;
    }

    public static boolean isDarkTheme(Composite parent) {
        Display display = parent.getDisplay();
        
        // 1. Check newer Eclipse API for system dark theme (fixes Mac SWT bugs)
        try {
            Boolean isDark = (Boolean) display.getClass().getMethod("isSystemDarkTheme").invoke(display);
            if (isDark != null) return isDark;
        } catch (Throwable t) {}

        // 2. Check Eclipse E4 CSS Theme
        try {
            Object themeEngine = display.getData("org.eclipse.e4.ui.css.swt.theme");
            if (themeEngine != null) {
                Object activeTheme = themeEngine.getClass().getMethod("getActiveTheme").invoke(themeEngine);
                if (activeTheme != null) {
                    String themeId = (String) activeTheme.getClass().getMethod("getId").invoke(activeTheme);
                    if (themeId != null) {
                        String lower = themeId.toLowerCase();
                        if (lower.contains("dark")) return true;
                        if (lower.contains("light")) return false;
                    }
                }
            }
        } catch (Throwable t) {}
        
        // 3. Fallback for macOS: Check native OS preference
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("mac")) {
            try {
                Process p = Runtime.getRuntime().exec(new String[] {"defaults", "read", "-g", "AppleInterfaceStyle"});
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                String line = reader.readLine();
                if (line != null && line.trim().equalsIgnoreCase("Dark")) {
                    return true;
                }
            } catch (Throwable t) {}
        }
        
        // 4. Fallback: Check parent composite background color
        Color bg = parent.getBackground();
        if (bg != null) {
            double brightness = getBrightness(bg);
            if (brightness < 128) return true;
        }
        
        return false;
    }

    private static double getBrightness(Color c) {
        return (c.getRed() * 299.0 + c.getGreen() * 587.0 + c.getBlue() * 114.0) / 1000.0;
    }

    /**
     * WORKAROUND: Eclipse Mac Dark Theme Bug.
     * In Eclipse Dark Theme on macOS, SWT.READ_ONLY Combos render their dropdown menus
     * natively but fail to invert the text color, making the items look black/dark-grey 
     * on a dark background (appearing disabled). 
     * 
     * This method uses internal Cocoa bindings to explicitly rewrite the NSAttributedString 
     * foreground color to pure white on the underlying NSPopUpButton menu items.
     */
    public static void fixComboDarkTheme(Combo combo) {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) return;

        try {
            NSPopUpButton button = new NSPopUpButton(combo.view.id);
            NSMenu menu = button.menu();
            if (menu == null) return;
            NSArray items = menu.itemArray();

            long count = items.count();
            for (int i = 0; i < count; i++) {
                NSMenuItem item = new NSMenuItem(items.objectAtIndex(i));

                // Build white attributed string
                NSMutableDictionary attrs = NSMutableDictionary.dictionaryWithCapacity(1);
                NSColor white = NSColor.colorWithDeviceRed(1.0, 1.0, 1.0, 1.0);
                attrs.setObject(white, OS.NSForegroundColorAttributeName);

                NSAttributedString attrTitle = new NSAttributedString();
                attrTitle.initWithString(item.title(), attrs);
                item.setAttributedTitle(attrTitle);
            }
        } catch (Throwable t) {
            // Ignore, likely not running on Cocoa or incompatible SWT version
        }
    }
}
