package com.burhankhanzada.opaquebehavioureditor.editor.text.languages;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;

public class CLanguageDef extends LanguageDef {
    public CLanguageDef() {
        super(LanguageMapping.LANG_C, "source.c", ".c",
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
            "//", "/*", "*/", "#");
    }
}
