package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public class JavaToCTranslator implements ITranslator {
    @Override
    public String translate(String sourceCode) {
        String result = sourceCode;
        result = result.replaceAll("\\.", "->");
        result = result.replaceAll("System->out->println\\((.*?)\\);", "printf(\"%d\\\\n\", $1);");
        result = result.replaceAll("\\bString\\b", "char*");
        result = result.replaceAll("\\bnull\\b", "NULL");
        result = result.replaceAll("\\bboolean\\b", "int");
        result = result.replaceAll("\\btrue\\b", "1");
        result = result.replaceAll("\\bfalse\\b", "0");
        result = result.replaceAll("new\\s+([A-Za-z0-9_]+)\\s*\\(\\)", "malloc(sizeof($1))");
        return result;
    }
}
