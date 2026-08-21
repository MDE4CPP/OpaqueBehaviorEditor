package de.tu_ilmenau.sse.opaquebehavioureditor.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageDef;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.ExpressionParser;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.TextScanState;

public class ModelValidator {

    public static final String[] COMMON_METHODS = {
        "add", "remove", "clear", "size", "empty", "front", "back", "insert", "erase", 
        "push_back", "pop_back", "begin", "end", "find", "count", "length", "substr", "at",
        // EMF standard methods
        "eSet", "eGet", "eIsSet", "eUnset", "eClass", "eContainer", "eContents",
        "eAllContents", "eCrossReferences", "eResource", "eIsProxy", "eResolveProxy"
    };

    /** The public API of MDE4CPP's Bag<T> (application/include/abstractDataTypes/Bag.hpp). */
    public static final String[] MDE4CPP_COLLECTION_METHODS = {
        "add", "at", "begin", "cbegin", "cend", "clear", "copy", "empty", "end", "erase",
        "find", "front", "includes", "index_of", "insert", "max_size", "push_back", "size"
    };

    /**
     * MDE4CPP models every multi-valued feature as a Bag<T>; Union, Subset and SubsetUnion are
     * the annotated variants and all derive from Bag, so they share its API.
     */
    static final String[] COLLECTION_TYPES = { "Bag", "Union", "Subset", "SubsetUnion" };

    public static boolean isCollectionType(String type) {
        if (type == null) return false;
        for (String c : COLLECTION_TYPES) {
            if (type.startsWith(c + "<")) return true;
        }
        return false;
    }

    private static final Pattern MEMBER_ACCESS_PATTERN = Pattern.compile("->[ \\t]*([A-Za-z0-9_]+)");

    private final ModelDictionary dictionary;

    public ModelValidator(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public ModelDictionary getDictionary() {
        return dictionary;
    }

    public List<TextRange> validateMemberAccess(String text, LanguageDef currentLangDef) {
        List<TextRange> errors = new ArrayList<>();
        if (currentLangDef == null || !currentLangDef.name.equals("CPP")) {
            return errors;
        }
        
        java.util.regex.Matcher m = MEMBER_ACCESS_PATTERN.matcher(text);
        boolean[] isCode = TextScanState.codeMask(text);
        
        while (m.find()) {
            // Commented-out or quoted code is not an error.
            if (!isCode[m.start()]) continue;
            String methodName = m.group(1);
            int methodOffset = m.start(1);
            int methodLength = methodName.length();
            
            String textBefore = text.substring(0, m.start() + 2);
            String rawType = ExpressionParser.resolveContextTypeFromText(textBefore, dictionary, text);
            
            if (rawType != null) {
                boolean isCollection = isCollectionType(rawType);

                if (rawType.startsWith("std::shared_ptr<")) {
                    rawType = rawType.substring(16, rawType.length() - 1);
                }
                
                boolean isValid = false;
                
                if (isCollection) {
                    for (String cm : MDE4CPP_COLLECTION_METHODS) {
                        if (cm.equals(methodName)) { isValid = true; break; }
                    }
                } else if (dictionary.getClassElements().containsKey(rawType) || dictionary.getTypeMembers().containsKey(rawType) || !dictionary.getAllTypeMembers(rawType).isEmpty()) {
                    Map<String, String> members = dictionary.getAllTypeMembers(rawType);
                    if (members != null && members.containsKey(methodName)) {
                        isValid = true;
                    }
                    if (!isValid) {
                        for (String cm : COMMON_METHODS) {
                            if (cm.equals(methodName)) { isValid = true; break; }
                        }
                    }
                } else {
                    isValid = true;
                }
                
                if (!isValid) {
                    errors.add(new TextRange(methodOffset, methodLength, "Method '" + methodName + "' is not defined in class '" + rawType + "'"));
                }
            }
        }
        return errors;
    }

    /** A statement made up of nothing but one identifier, e.g. {@code bookMasadsad;}. */
    private static final Pattern LONE_IDENTIFIER_STATEMENT =
            Pattern.compile("(?m)(?:^|[;{}])\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*;");

    /**
     * Reports statements that consist of a single identifier. Such a statement evaluates a name and
     * throws the result away, so it is always a mistake - most often a half-typed line like
     * {@code bookMasadsad;}. Whether the name is known decides only the wording.
     *
     * <p>Deliberately narrow: a general undeclared-identifier check is not possible here, because
     * operation parameters, impl-only member fields and everything coming from the includes are
     * invisible to the editor.
     */
    public List<TextRange> validateStatements(String text, LanguageDef currentLangDef) {
        List<TextRange> errors = new ArrayList<>();
        if (text == null || currentLangDef == null || !currentLangDef.name.equals("CPP")) {
            return errors;
        }

        boolean[] isCode = TextScanState.codeMask(text);
        java.util.Set<String> known = new java.util.HashSet<>(dictionary.getAutocompleteWords());
        known.addAll(ExpressionParser.declaredVariables(text, text.length()));
        for (String k : currentLangDef.keywords) known.add(k);
        for (String t : currentLangDef.types) known.add(t);
        known.add(AbstractModelHarvester.FACTORY_TYPE);

        java.util.regex.Matcher m = LONE_IDENTIFIER_STATEMENT.matcher(text);
        while (m.find()) {
            int at = m.start(1);
            if (!isCode[at]) continue;
            String name = m.group(1);
            // `break;`, `continue;`, `return;` are statements in their own right
            if (isKeyword(name, currentLangDef)) continue;
            String message = known.contains(name)
                    ? "Statement '" + name + ";' has no effect"
                    : "'" + name + "' is not declared in this body or the model";
            errors.add(new TextRange(at, name.length(), message));
        }
        return errors;
    }

    private static boolean isKeyword(String word, LanguageDef langDef) {
        for (String k : langDef.keywords) {
            if (k.equals(word)) return true;
        }
        return false;
    }

    /**
     * Validates basic syntax like unmatched brackets.
     */
    public List<TextRange> validateSyntax(String text, LanguageDef currentLangDef) {
        List<TextRange> errors = new ArrayList<>();
        if (text == null || text.isEmpty()) return errors;

        java.util.Stack<Integer> openParen = new java.util.Stack<>();
        java.util.Stack<Integer> openBrace = new java.util.Stack<>();
        java.util.Stack<Integer> openBracket = new java.util.Stack<>();

        TextScanState state = new TextScanState();

        for (int i = 0; i < text.length(); i++) {
            boolean wasIgnored = state.isIgnored();
            int skip = state.process(text, i);
            
            if (wasIgnored || state.isIgnored()) {
                i += skip;
                continue;
            }
            
            char c = text.charAt(i);

            if (c == '(') openParen.push(i);
            else if (c == '{') openBrace.push(i);
            else if (c == '[') openBracket.push(i);
            else if (c == ')') {
                if (openParen.isEmpty()) errors.add(new TextRange(i, 1, "Unmatched closing ')'"));
                else openParen.pop();
            }
            else if (c == '}') {
                if (openBrace.isEmpty()) errors.add(new TextRange(i, 1, "Unmatched closing '}'"));
                else openBrace.pop();
            }
            else if (c == ']') {
                if (openBracket.isEmpty()) errors.add(new TextRange(i, 1, "Unmatched closing ']'"));
                else openBracket.pop();
            }
            i += skip;
        }

        while (!openParen.isEmpty()) errors.add(new TextRange(openParen.pop(), 1, "Unclosed opening '('"));
        while (!openBrace.isEmpty()) errors.add(new TextRange(openBrace.pop(), 1, "Unclosed opening '{'"));
        while (!openBracket.isEmpty()) errors.add(new TextRange(openBracket.pop(), 1, "Unclosed opening '['"));

        return errors;
    }


}
