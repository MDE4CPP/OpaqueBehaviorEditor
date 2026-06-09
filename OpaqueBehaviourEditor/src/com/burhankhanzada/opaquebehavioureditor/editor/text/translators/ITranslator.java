package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public interface ITranslator {
    /**
     * Translates the given source code to the target language.
     * 
     * @param sourceCode The original code snippet.
     * @return The translated code.
     */
    String translate(String sourceCode);
}
