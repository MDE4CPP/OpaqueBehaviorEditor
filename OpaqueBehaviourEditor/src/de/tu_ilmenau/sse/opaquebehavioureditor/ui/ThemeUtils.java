package de.tu_ilmenau.sse.opaquebehavioureditor.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

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
     * This method uses internal Cocoa bindings via reflection to explicitly rewrite the 
     * NSAttributedString foreground color to pure white on the underlying NSPopUpButton menu items.
     * Using reflection prevents NoClassDefFoundError on Windows and Linux where cocoa packages do not exist.
     */
    public static void fixComboDarkTheme(Combo combo) {
        if (!System.getProperty("os.name").toLowerCase().contains("mac")) return;

        try {
            java.lang.reflect.Field viewField = combo.getClass().getField("view");
            Object viewObj = viewField.get(combo);
            if (viewObj == null) return;

            java.lang.reflect.Field idField = viewObj.getClass().getField("id");
            long viewId = idField.getLong(viewObj);
            if (viewId == 0) return;

            Class<?> idClass = Class.forName("org.eclipse.swt.internal.cocoa.id");
            Class<?> nsPopUpButtonClass = Class.forName("org.eclipse.swt.internal.cocoa.NSPopUpButton");
            Object buttonObj = nsPopUpButtonClass.getConstructor(long.class).newInstance(viewId);

            Object menuObj = nsPopUpButtonClass.getMethod("menu").invoke(buttonObj);
            if (menuObj == null) return;

            Class<?> nsMenuClass = Class.forName("org.eclipse.swt.internal.cocoa.NSMenu");
            Object itemsObj = nsMenuClass.getMethod("itemArray").invoke(menuObj);
            if (itemsObj == null) return;

            Class<?> nsArrayClass = Class.forName("org.eclipse.swt.internal.cocoa.NSArray");
            long count = ((Number) nsArrayClass.getMethod("count").invoke(itemsObj)).longValue();

            java.lang.reflect.Method objectAtIndexMethod = nsArrayClass.getMethod("objectAtIndex", long.class);

            Class<?> nsMenuItemClass = Class.forName("org.eclipse.swt.internal.cocoa.NSMenuItem");
            java.lang.reflect.Constructor<?> nsMenuItemCtor = nsMenuItemClass.getConstructor(idClass);
            java.lang.reflect.Method titleMethod = nsMenuItemClass.getMethod("title");

            Class<?> nsAttrStringClass = Class.forName("org.eclipse.swt.internal.cocoa.NSAttributedString");
            java.lang.reflect.Method setAttributedTitleMethod = nsMenuItemClass.getMethod("setAttributedTitle", nsAttrStringClass);

            Class<?> nsMutableDictClass = Class.forName("org.eclipse.swt.internal.cocoa.NSMutableDictionary");
            java.lang.reflect.Method dictWithCapacityMethod = null;
            for (java.lang.reflect.Method m : nsMutableDictClass.getMethods()) {
                if (m.getName().equals("dictionaryWithCapacity")) {
                    dictWithCapacityMethod = m;
                    break;
                }
            }
            if (dictWithCapacityMethod == null) return;

            Object attrsObj = dictWithCapacityMethod.invoke(null, 1);

            Class<?> nsColorClass = Class.forName("org.eclipse.swt.internal.cocoa.NSColor");
            java.lang.reflect.Method colorWithDeviceRedMethod = nsColorClass.getMethod("colorWithDeviceRed", double.class, double.class, double.class, double.class);
            Object whiteColorObj = colorWithDeviceRedMethod.invoke(null, 1.0, 1.0, 1.0, 1.0);

            Class<?> osClass = Class.forName("org.eclipse.swt.internal.cocoa.OS");
            Object fgAttrObj = osClass.getField("NSForegroundColorAttributeName").get(null);

            java.lang.reflect.Method setObjectMethod = null;
            for (java.lang.reflect.Method m : nsMutableDictClass.getMethods()) {
                if (m.getName().equals("setObject") && m.getParameterTypes().length == 2) {
                    setObjectMethod = m;
                    break;
                }
            }
            if (setObjectMethod == null) return;
            setObjectMethod.invoke(attrsObj, whiteColorObj, fgAttrObj);

            Class<?> nsStringClass = Class.forName("org.eclipse.swt.internal.cocoa.NSString");
            java.lang.reflect.Constructor<?> nsAttrStringCtor = nsAttrStringClass.getConstructor();
            java.lang.reflect.Method initWithStringMethod = nsAttrStringClass.getMethod("initWithString", nsStringClass, nsMutableDictClass);

            for (long i = 0; i < count; i++) {
                Object itemHandle = objectAtIndexMethod.invoke(itemsObj, i);
                Object menuItemObj = nsMenuItemCtor.newInstance(itemHandle);
                Object titleObj = titleMethod.invoke(menuItemObj);

                Object attrTitleObj = nsAttrStringCtor.newInstance();
                initWithStringMethod.invoke(attrTitleObj, titleObj, attrsObj);
                setAttributedTitleMethod.invoke(menuItemObj, attrTitleObj);
            }
        } catch (Throwable t) {
            // Ignore, likely not running on Cocoa or incompatible SWT version
        }
    }
}

