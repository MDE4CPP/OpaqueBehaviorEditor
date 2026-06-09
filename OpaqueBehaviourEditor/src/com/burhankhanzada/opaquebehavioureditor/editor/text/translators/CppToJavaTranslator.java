package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public class CppToJavaTranslator implements ITranslator {
    @Override
    public String translate(String sourceCode) {
        String result = sourceCode;
        // Complex patterns first before we destroy '::'
        result = result.replaceAll("std::(?:shared|weak|unique)_ptr<\\s*([A-Za-z0-9_]+)\\s*>", "$1");
        result = result.replaceAll("std::string", "String");
        result = result.replaceAll("std::cout\\s*<<\\s*(.*?)\\s*<<\\s*std::endl\\s*;", "System.out.println($1);");
        
        // Simple symbol replacements
        result = result.replaceAll("->", ".");
        result = result.replaceAll("::", ".");
        result = result.replaceAll("\\bnullptr\\b", "null");
        result = result.replaceAll("\\bbool\\b", "boolean");
        result = result.replaceAll("\\bconst\\b", "final");
        return result;
    }
}
