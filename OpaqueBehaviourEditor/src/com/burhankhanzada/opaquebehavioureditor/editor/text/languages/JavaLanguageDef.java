package com.burhankhanzada.opaquebehavioureditor.editor.text.languages;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;

public class JavaLanguageDef extends LanguageDef {
    public JavaLanguageDef() {
        super(LanguageMapping.LANG_JAVA, "source.java", ".java",
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
            "//", "/*", "*/", null);
    }
}
