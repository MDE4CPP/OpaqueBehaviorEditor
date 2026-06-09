package com.burhankhanzada.opaquebehavioureditor.editor.text.languages;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;

public class CppLanguageDef extends LanguageDef {
    public CppLanguageDef() {
        super(LanguageMapping.LANG_CPP, "source.cpp", ".cpp",
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
            "//", "/*", "*/", "#");
    }
}
