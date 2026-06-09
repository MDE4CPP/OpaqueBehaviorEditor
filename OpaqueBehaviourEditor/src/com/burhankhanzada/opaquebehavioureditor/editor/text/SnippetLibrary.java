package com.burhankhanzada.opaquebehavioureditor.editor.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.burhankhanzada.opaquebehavioureditor.utils.PluginLogger;

public class SnippetLibrary {

    public static class Snippet {
        public final String keyword;
        public final String label;
        public final String template;
        
        public Snippet(String keyword, String label, String template) {
            this.keyword = keyword;
            this.label = label;
            this.template = template;
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

        // 1. Load default snippets
        try (java.io.InputStream defaultIn = SnippetLibrary.class.getResourceAsStream("default_snippets.properties");
             java.io.InputStreamReader reader = new java.io.InputStreamReader(defaultIn, java.nio.charset.StandardCharsets.UTF_8)) {
            if (defaultIn != null) {
                props.load(reader);
            }
        } catch (IOException e) {
            PluginLogger.logError("Failed to load default_snippets.properties", e);
        }

        // 2. Merge user snippets on top
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file);
                 java.io.InputStreamReader reader = new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)) {
                props.load(reader);
            } catch (IOException e) {
                PluginLogger.logError("Failed to load user snippets.properties", e);
            }
        }

        try {
            List<String> keywords = new ArrayList<>();
            for (String key : props.stringPropertyNames()) {
                if (key.endsWith(".template")) {
                    keywords.add(key.substring(0, key.length() - 9));
                }
            }

            for (String keyword : keywords) {
                String label = props.getProperty(keyword + ".label", keyword + " (Snippet)");
                String template = props.getProperty(keyword + ".template", "");
                
                if (!template.isEmpty()) {
                    list.add(new Snippet(keyword, label, template));
                }
            }
        } catch (Exception e) {
            PluginLogger.logError("Failed to process merged snippets", e);
        }
        return list;
    }
}
