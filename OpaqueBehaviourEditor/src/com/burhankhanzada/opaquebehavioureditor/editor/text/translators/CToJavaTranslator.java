package com.burhankhanzada.opaquebehavioureditor.editor.text.translators;

public class CToJavaTranslator implements ITranslator {
    @Override
    public String translate(String sourceCode) {
        String result = sourceCode;
        result = result.replaceAll("->", ".");
        result = result.replaceAll("char\\*", "String");
        result = result.replaceAll("\\bNULL\\b", "null");
        result = result.replaceAll("printf\\(\"%d\\\\n\"\\s*,\\s*(.*?)\\);", "System.out.println($1);");
        result = result.replaceAll("malloc\\s*\\(\\s*sizeof\\s*\\(\\s*([A-Za-z0-9_]+)\\s*\\)\\s*\\)", "new $1()");
        
        // Remove pointer asterisks from declarations (e.g. "Library* lib" -> "Library lib")
        result = result.replaceAll("([A-Za-z0-9_]+)\\s*\\*\\s+([A-Za-z0-9_]+)", "$1 $2");
        return result;
    }
}
