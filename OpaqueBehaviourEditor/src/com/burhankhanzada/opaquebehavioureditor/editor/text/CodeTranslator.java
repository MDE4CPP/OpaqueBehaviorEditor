package com.burhankhanzada.opaquebehavioureditor.editor.text;


import java.util.HashMap;
import java.util.Map;

import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CToCppTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CToJavaTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CppToCTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CppToJavaTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.ITranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.JavaToCTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.JavaToCppTranslator;

/**
 * Utility class to perform best-effort regex-based translation 
 * between supported programming languages.
 */
public class CodeTranslator {

    private static final Map<String, ITranslator> translators = new HashMap<>();

    static {
        translators.put(LanguageMapping.LANG_CPP.toUpperCase() + "->" + LanguageMapping.LANG_JAVA.toUpperCase(), new CppToJavaTranslator());
        translators.put(LanguageMapping.LANG_JAVA.toUpperCase() + "->" + LanguageMapping.LANG_CPP.toUpperCase(), new JavaToCppTranslator());
        translators.put(LanguageMapping.LANG_CPP.toUpperCase() + "->" + LanguageMapping.LANG_C.toUpperCase(), new CppToCTranslator());
        translators.put(LanguageMapping.LANG_C.toUpperCase() + "->" + LanguageMapping.LANG_CPP.toUpperCase(), new CToCppTranslator());
        translators.put(LanguageMapping.LANG_JAVA.toUpperCase() + "->" + LanguageMapping.LANG_C.toUpperCase(), new JavaToCTranslator());
        translators.put(LanguageMapping.LANG_C.toUpperCase() + "->" + LanguageMapping.LANG_JAVA.toUpperCase(), new CToJavaTranslator());
    }

    /**
     * Translates the given code from the source language to the target language.
     * 
     * @param code       The source code snippet.
     * @param sourceLang The language to translate from (e.g. "CPP", "Java", "C").
     * @param targetLang The language to translate to (e.g. "CPP", "Java", "C").
     * @return The translated code snippet.
     */
    public static String translate(String code, String sourceLang, String targetLang) {
        if (code == null || code.isBlank()) return "";
        if (sourceLang == null || targetLang == null) return code;
        
        String src = sourceLang.toUpperCase();
        String tgt = targetLang.toUpperCase();
        
        if (src.equals(tgt)) return code;

        String result = code;
        String key = src + "->" + tgt;

        ITranslator translator = translators.get(key);
        if (translator != null) {
            result = translator.translate(code);
        }

        // Fix basic spacing issues (e.g. lib=factory -> lib = factory)
        // This is a naive cleanup for common missing spaces around assignment.
        result = result.replaceAll("([^\\s=!<>+\\-*/%])=([^\\s=])", "$1 = $2");

        return result;
    }
}
