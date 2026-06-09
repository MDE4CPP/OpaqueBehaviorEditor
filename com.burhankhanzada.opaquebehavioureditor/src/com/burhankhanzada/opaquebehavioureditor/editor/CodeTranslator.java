package umlopaquebehaviourbodyeditor.editor;

import umlopaquebehaviourbodyeditor.ui.*;
import umlopaquebehaviourbodyeditor.editor.*;
import umlopaquebehaviourbodyeditor.model.*;


/**
 * Utility class to perform best-effort regex-based translation 
 * between supported programming languages.
 */
public class CodeTranslator {

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

        if (src.equals("CPP") && tgt.equals("JAVA")) {
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
        } 
        else if (src.equals("JAVA") && tgt.equals("CPP")) {
            // Naive assumption: most method calls in MDE4CPP are via pointers
            result = result.replaceAll("\\.", "->");
            result = result.replaceAll("\\bString\\b", "std::string");
            result = result.replaceAll("System->out->println\\((.*?)\\);", "std::cout << $1 << std::endl;");
            result = result.replaceAll("\\bnull\\b", "nullptr");
            result = result.replaceAll("\\bboolean\\b", "bool");
            result = result.replaceAll("\\bfinal\\b", "const");
            
            // Wrap capitalized object declarations in std::shared_ptr (e.g. "Library lib =" -> "std::shared_ptr<Library> lib =")
            result = result.replaceAll("\\b([A-Z][A-Za-z0-9_]*)\\s+([a-zA-Z0-9_]+)\\s*=", "std::shared_ptr<$1> $2 =");
        }
        else if (src.equals("CPP") && tgt.equals("C")) {
            result = result.replaceAll("std::(?:shared|weak|unique)_ptr<\\s*([A-Za-z0-9_]+)\\s*>", "$1*");
            result = result.replaceAll("std::string", "char*");
            result = result.replaceAll("std::cout\\s*<<\\s*(.*?)\\s*<<\\s*std::endl\\s*;", "printf(\"%d\\\\n\", $1);");
            result = result.replaceAll("\\bnullptr\\b", "NULL");
            result = result.replaceAll("\\bbool\\b", "int");
            result = result.replaceAll("\\btrue\\b", "1");
            result = result.replaceAll("\\bfalse\\b", "0");
            result = result.replaceAll("\\bclass\\b", "struct");
            result = result.replaceAll("new\\s+([A-Za-z0-9_]+)\\s*\\(\\)", "malloc(sizeof($1))");
        }
        else if (src.equals("C") && tgt.equals("CPP")) {
            result = result.replaceAll("char\\*", "std::string");
            result = result.replaceAll("\\bNULL\\b", "nullptr");
            result = result.replaceAll("malloc\\s*\\(\\s*sizeof\\s*\\(\\s*([A-Za-z0-9_]+)\\s*\\)\\s*\\)", "new $1()");
            
            // Convert C-style pointers of objects to std::shared_ptr (e.g. "Library* lib" -> "std::shared_ptr<Library> lib")
            result = result.replaceAll("\\b([A-Z][A-Za-z0-9_]*)\\s*\\*\\s+([a-zA-Z0-9_]+)", "std::shared_ptr<$1> $2");
        }
        else if (src.equals("JAVA") && tgt.equals("C")) {
            result = result.replaceAll("\\.", "->");
            result = result.replaceAll("System->out->println\\((.*?)\\);", "printf(\"%d\\\\n\", $1);");
            result = result.replaceAll("\\bString\\b", "char*");
            result = result.replaceAll("\\bnull\\b", "NULL");
            result = result.replaceAll("\\bboolean\\b", "int");
            result = result.replaceAll("\\btrue\\b", "1");
            result = result.replaceAll("\\bfalse\\b", "0");
            result = result.replaceAll("new\\s+([A-Za-z0-9_]+)\\s*\\(\\)", "malloc(sizeof($1))");
        }
        else if (src.equals("C") && tgt.equals("JAVA")) {
            result = result.replaceAll("->", ".");
            result = result.replaceAll("char\\*", "String");
            result = result.replaceAll("\\bNULL\\b", "null");
            result = result.replaceAll("printf\\(\"%d\\\\n\"\\s*,\\s*(.*?)\\);", "System.out.println($1);");
            result = result.replaceAll("malloc\\s*\\(\\s*sizeof\\s*\\(\\s*([A-Za-z0-9_]+)\\s*\\)\\s*\\)", "new $1()");
            
            // Remove pointer asterisks from declarations (e.g. "Library* lib" -> "Library lib")
            result = result.replaceAll("([A-Za-z0-9_]+)\\s*\\*\\s+([A-Za-z0-9_]+)", "$1 $2");
        }

        // Fix basic spacing issues (e.g. lib=factory -> lib = factory)
        // This is a naive cleanup for common missing spaces around assignment.
        result = result.replaceAll("([^\\s=!<>+\\-*/%])=([^\\s=])", "$1 = $2");

        return result;
    }
}
