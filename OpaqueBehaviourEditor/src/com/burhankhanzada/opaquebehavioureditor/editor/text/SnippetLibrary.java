package com.burhankhanzada.opaquebehavioureditor.editor.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.burhankhanzada.opaquebehavioureditor.utils.PluginLogger;

public class SnippetLibrary {

    public static class Snippet {
        public final String keyword;
        public final String label;
        public final String template;
        public final String placeholder;
        
        public Snippet(String keyword, String label, String template, String placeholder) {
            this.keyword = keyword;
            this.label = label;
            this.template = template;
            this.placeholder = placeholder;
        }
    }

    private static List<Snippet> cachedSnippets = null;
    private static long lastModified = 0;

    public static File getSnippetsFile() {
        String homeDir = System.getProperty("user.home");
        return new File(homeDir, ".opaque_snippets.properties");
    }

    public static List<Snippet> getSnippets() {
        File file = getSnippetsFile();
        
        if (!file.exists()) {
            createDefaultSnippetsFile(file);
        }

        if (cachedSnippets == null || file.lastModified() > lastModified) {
            cachedSnippets = loadSnippetsFromFile(file);
            lastModified = file.lastModified();
        }

        return cachedSnippets;
    }

    private static void createDefaultSnippetsFile(File file) {
        try (java.io.InputStream in = SnippetLibrary.class.getResourceAsStream("default_snippets.properties");
             FileOutputStream out = new FileOutputStream(file)) {
            if (in != null) {
                in.transferTo(out);
            } else {
                PluginLogger.logError("Could not find default_snippets.properties in classpath.", null);
            }
        } catch (IOException e) {
            PluginLogger.logError("Failed to create default snippets.properties", e);
        }
    }

    private static List<Snippet> loadSnippetsFromFile(File file) {
        List<Snippet> list = new ArrayList<>();
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(file);
             java.io.InputStreamReader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
            props.load(reader);
            
            // Extract distinct root keywords (e.g. "create", "for", "cast")
            Set<String> keywords = new TreeSet<>();
            for (Object keyObj : props.keySet()) {
                String key = (String) keyObj;
                int dotIdx = key.indexOf('.');
                if (dotIdx > 0) {
                    keywords.add(key.substring(0, dotIdx));
                }
            }

            for (String keyword : keywords) {
                String label = props.getProperty(keyword + ".label", keyword + " (Snippet)");
                String placeholder = props.getProperty(keyword + ".placeholder", "Type");
                String template = props.getProperty(keyword + ".template", "");
                
                if (!template.isEmpty()) {
                    list.add(new Snippet(keyword, label, template, placeholder));
                }
            }
        } catch (IOException e) {
            PluginLogger.logError("Failed to load snippets.properties", e);
            // Fallback to defaults
            list.add(new Snippet("create", "create (Snippet)", "std::shared_ptr<Type> item = factory->createType();", "Type"));
            list.add(new Snippet("for", "for (Snippet)", "for(std::shared_ptr<Type> item : *collection) {\n    \n}", "Type"));
            list.add(new Snippet("cast", "cast (Snippet)", "std::shared_ptr<Type> casted = std::dynamic_pointer_cast<Type>(item);", "Type"));
        }
        return list;
    }
}
