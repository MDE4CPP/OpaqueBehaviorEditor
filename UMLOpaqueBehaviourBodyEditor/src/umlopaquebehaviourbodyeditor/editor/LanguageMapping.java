package umlopaquebehaviourbodyeditor.editor;

import umlopaquebehaviourbodyeditor.ui.*;
import umlopaquebehaviourbodyeditor.editor.*;
import umlopaquebehaviourbodyeditor.model.*;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps UML OpaqueBehaviour language property strings to syntax highlighting
 * configuration: keywords, types, TM4E scope names, and file extensions.
 */
public final class LanguageMapping {

    private LanguageMapping() {
        // Utility class
    }

    /** All known language definitions, keyed by canonical name. */
    private static final Map<String, LanguageDef> LANGUAGES = new LinkedHashMap<>();

    static {
        // ---- C ----
        LANGUAGES.put("C", new LanguageDef("C", "source.c", ".c",
            new String[] {
                "auto", "break", "case", "char", "const", "continue",
                "default", "do", "double", "else", "enum", "extern",
                "float", "for", "goto", "if", "inline", "int", "long",
                "register", "restrict", "return", "short", "signed",
                "sizeof", "static", "struct", "switch", "typedef",
                "union", "unsigned", "void", "volatile", "while",
                "_Alignas", "_Alignof", "_Atomic", "_Bool", "_Complex",
                "_Generic", "_Imaginary", "_Noreturn", "_Static_assert",
                "_Thread_local", "NULL", "true", "false"
            },
            new String[] {
                "size_t", "ptrdiff_t", "int8_t", "int16_t", "int32_t",
                "int64_t", "uint8_t", "uint16_t", "uint32_t", "uint64_t",
                "FILE", "va_list", "wchar_t", "time_t", "clock_t",
                "pid_t", "ssize_t", "off_t"
            },
            "//", "/*", "*/", "#"));

        // ---- CPP ----
        LANGUAGES.put("CPP", new LanguageDef("CPP", "source.cpp", ".cpp",
            new String[] {
                "alignas", "alignof", "and", "and_eq", "asm", "auto",
                "bitand", "bitor", "bool", "break", "case", "catch",
                "char", "char8_t", "char16_t", "char32_t", "class",
                "compl", "concept", "const", "const_cast", "consteval",
                "constexpr", "constinit", "continue", "co_await",
                "co_return", "co_yield", "decltype", "default", "delete",
                "do", "double", "dynamic_cast", "else", "enum", "explicit",
                "export", "extern", "false", "float", "for", "friend",
                "goto", "if", "inline", "int", "long", "mutable",
                "namespace", "new", "noexcept", "not", "not_eq",
                "nullptr", "operator", "or", "or_eq", "private",
                "protected", "public", "register", "reinterpret_cast",
                "requires", "return", "short", "signed", "sizeof",
                "static", "static_assert", "static_cast", "struct",
                "switch", "template", "this", "thread_local", "throw",
                "true", "try", "typedef", "typeid", "typename", "union",
                "unsigned", "using", "virtual", "void", "volatile",
                "wchar_t", "while", "xor", "xor_eq"
            },
            new String[] {
                "size_t", "ptrdiff_t", "int8_t", "int16_t", "int32_t",
                "int64_t", "uint8_t", "uint16_t", "uint32_t", "uint64_t",
                "string", "vector", "map", "set", "list", "deque",
                "shared_ptr", "unique_ptr", "weak_ptr", "optional",
                "pair", "tuple", "array", "unordered_map", "unordered_set"
            },
            "//", "/*", "*/", "#"));

        // ---- Java ----
        LANGUAGES.put("Java", new LanguageDef("Java", "source.java", ".java",
            new String[] {
                "abstract", "assert", "boolean", "break", "byte", "case",
                "catch", "char", "class", "continue", "default", "do",
                "double", "else", "enum", "extends", "final", "finally",
                "float", "for", "goto", "if", "implements", "import",
                "instanceof", "int", "interface", "long", "native", "new",
                "package", "private", "protected", "public", "return",
                "short", "static", "strictfp", "super", "switch",
                "synchronized", "this", "throw", "throws", "transient",
                "try", "void", "volatile", "while", "true", "false",
                "null", "var", "yield", "record", "sealed", "permits",
                "non-sealed"
            },
            new String[] {
                "String", "Integer", "Long", "Double", "Float", "Boolean",
                "Character", "Byte", "Short", "Object", "Class", "System",
                "List", "Map", "Set", "Collection", "ArrayList", "HashMap",
                "HashSet", "Optional", "Stream", "Comparable", "Iterable",
                "Iterator", "Runnable", "Callable", "Thread", "Exception",
                "RuntimeException", "Override", "Deprecated", "SuppressWarnings"
            },
            "//", "/*", "*/", null));
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
            case "cpp", "c++", "c++11", "c++14", "c++17", "c++20", "c++23" -> LANGUAGES.get("CPP");
            case "java" -> LANGUAGES.get("Java");
            case "c89", "c99", "c11", "c17", "c23" -> LANGUAGES.get("C");
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

    /**
     * Holds all syntax-relevant metadata for a single programming language.
     */
    public static final class LanguageDef {
        public final String name;
        public final String scopeName;
        public final String fileExtension;
        public final String[] keywords;
        public final String[] types;
        public final String singleLineComment;
        public final String multiLineCommentStart;
        public final String multiLineCommentEnd;
        public final String preprocessorPrefix;

        LanguageDef(String name, String scopeName, String fileExtension,
                    String[] keywords, String[] types,
                    String singleLineComment,
                    String multiLineCommentStart, String multiLineCommentEnd,
                    String preprocessorPrefix) {
            this.name = name;
            this.scopeName = scopeName;
            this.fileExtension = fileExtension;
            this.keywords = keywords;
            this.types = types;
            this.singleLineComment = singleLineComment;
            this.multiLineCommentStart = multiLineCommentStart;
            this.multiLineCommentEnd = multiLineCommentEnd;
            this.preprocessorPrefix = preprocessorPrefix;
        }

        public boolean isPlainText() {
            return name.isEmpty();
        }
    }
}
