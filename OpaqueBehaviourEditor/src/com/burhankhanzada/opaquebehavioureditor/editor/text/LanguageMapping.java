package com.burhankhanzada.opaquebehavioureditor.editor.text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.burhankhanzada.opaquebehavioureditor.editor.text.languages.CLanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.languages.CppLanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.languages.JavaLanguageDef;

/**
 * Maps UML OpaqueBehaviour language property strings to syntax highlighting
 * configuration: keywords, types, TM4E scope names, and file extensions.
 */
public final class LanguageMapping {

    private LanguageMapping() {
        // Utility class
    }

    public static final String LANG_C = "C";
    public static final String LANG_CPP = "CPP";
    public static final String LANG_JAVA = "Java";

    /** All known language definitions, keyed by canonical name. */
    private static final Map<String, LanguageDef> LANGUAGES = new LinkedHashMap<>();

    static {
        LANGUAGES.put(LANG_C, new CLanguageDef());
        LANGUAGES.put(LANG_CPP, new CppLanguageDef());
        LANGUAGES.put(LANG_JAVA, new JavaLanguageDef());
    }

    /**
     * Returns the language definition for the given language name.
     * Performs case-insensitive matching and common alias resolution.
     *
     * @param language the language name from the UML model (e.g. "CPP", "cpp", "Java")
     * @return the matching LanguageDef, or a plain-text fallback if not found
     */
    public static LanguageDef getLanguageDef(String language) {
        if (language == null || language.isBlank()) {
            return PLAIN_TEXT;
        }

        // Direct lookup
        LanguageDef def = LANGUAGES.get(language);
        if (def != null) return def;

        // Case-insensitive lookup
        for (Map.Entry<String, LanguageDef> entry : LANGUAGES.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(language)) {
                return entry.getValue();
            }
        }

        // Common aliases
        String lower = language.toLowerCase();
        return switch (lower) {
            case "cpp", "c++", "c++11", "c++14", "c++17", "c++20", "c++23" -> LANGUAGES.get(LANG_CPP);
            case "java" -> LANGUAGES.get(LANG_JAVA);
            case "c89", "c99", "c11", "c17", "c23" -> LANGUAGES.get(LANG_C);
            default -> PLAIN_TEXT;
        };
    }

    /** Returns all registered language names (for combo box items). */
    public static String[] getAllLanguageNames() {
        return LANGUAGES.keySet().toArray(String[]::new);
    }

    /** Returns a read-only map of all language definitions. */
    public static Map<String, LanguageDef> getAllLanguages() {
        return Collections.unmodifiableMap(LANGUAGES);
    }

    /** Plain-text fallback with no syntax rules. */
    public static final LanguageDef PLAIN_TEXT = new LanguageDef(
        "", "", ".txt",
        new String[0], new String[0],
        null, null, null, null
    );
}
