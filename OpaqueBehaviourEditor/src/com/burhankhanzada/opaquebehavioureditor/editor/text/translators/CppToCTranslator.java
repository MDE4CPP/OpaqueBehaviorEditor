package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public class CppToCTranslator implements ITranslator {
    @Override
    public String translate(String sourceCode) {
        String result = sourceCode;
        result = result.replaceAll("std::(?:shared|weak|unique)_ptr<\\s*([A-Za-z0-9_]+)\\s*>", "$1*");
        result = result.replaceAll("std::string", "char*");
        result = result.replaceAll("std::cout\\s*<<\\s*(.*?)\\s*<<\\s*std::endl\\s*;", "printf(\"%d\\\\n\", $1);");
        result = result.replaceAll("\\bnullptr\\b", "NULL");
        result = result.replaceAll("\\bbool\\b", "int");
        result = result.replaceAll("\\btrue\\b", "1");
        result = result.replaceAll("\\bfalse\\b", "0");
        result = result.replaceAll("\\bclass\\b", "struct");
        result = result.replaceAll("new\\s+([A-Za-z0-9_]+)\\s*\\(\\)", "malloc(sizeof($1))");
        return result;
    }
}
