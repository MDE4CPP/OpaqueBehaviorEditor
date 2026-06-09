package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public class JavaToCppTranslator implements ITranslator {
    @Override
    public String translate(String sourceCode) {
        String result = sourceCode;
        // Naive assumption: most method calls in MDE4CPP are via pointers
        result = result.replaceAll("\\.", "->");
        result = result.replaceAll("\\bString\\b", "std::string");
        result = result.replaceAll("System->out->println\\((.*?)\\);", "std::cout << $1 << std::endl;");
        result = result.replaceAll("\\bnull\\b", "nullptr");
        result = result.replaceAll("\\bboolean\\b", "bool");
        result = result.replaceAll("\\bfinal\\b", "const");
        
        // Wrap capitalized object declarations in std::shared_ptr (e.g. "Library lib =" -> "std::shared_ptr<Library> lib =")
        result = result.replaceAll("\\b([A-Z][A-Za-z0-9_]*)\\s+([a-zA-Z0-9_]+)\\s*=", "std::shared_ptr<$1> $2 =");
        return result;
    }
}
