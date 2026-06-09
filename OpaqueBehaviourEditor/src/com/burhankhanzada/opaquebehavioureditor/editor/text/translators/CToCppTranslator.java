package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public class CToCppTranslator implements ITranslator {
    @Override
    public String translate(String sourceCode) {
        String result = sourceCode;
        result = result.replaceAll("char\\*", "std::string");
        result = result.replaceAll("\\bNULL\\b", "nullptr");
        result = result.replaceAll("malloc\\s*\\(\\s*sizeof\\s*\\(\\s*([A-Za-z0-9_]+)\\s*\\)\\s*\\)", "new $1()");
        
        // Convert C-style pointers of objects to std::shared_ptr (e.g. "Library* lib" -> "std::shared_ptr<Library> lib")
        result = result.replaceAll("\\b([A-Z][A-Za-z0-9_]*)\\s*\\*\\s+([a-zA-Z0-9_]+)", "std::shared_ptr<$1> $2");
        return result;
    }
}
