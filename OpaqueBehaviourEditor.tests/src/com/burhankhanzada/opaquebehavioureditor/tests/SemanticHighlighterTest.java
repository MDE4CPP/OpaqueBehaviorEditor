package com.burhankhanzada.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.burhankhanzada.opaquebehavioureditor.editor.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.editor.SemanticHighlighter;
import com.burhankhanzada.opaquebehavioureditor.model.TextRange;
import com.burhankhanzada.opaquebehavioureditor.model.UmlModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.UmlModelValidator;

public class SemanticHighlighterTest {

    private UmlModelDictionary dictionary;
    private SemanticHighlighter highlighter;
    private UmlModelValidator validator;
    private LanguageMapping.LanguageDef cppLangDef;

    @Before
    public void setUp() {
        dictionary = new UmlModelDictionary();
        // Add some dummy UML types and methods
        dictionary.classElements.put("Library", new java.util.HashMap<>());
        dictionary.classElements.put("Book", new java.util.HashMap<>());
        
        java.util.Map<String, String> libraryMethods = new java.util.HashMap<>();
        libraryMethods.put("createBook", "uml::Operation");
        libraryMethods.put("printLibrary", "uml::Operation");
        dictionary.typeMembers.put("Library", libraryMethods);

        highlighter = new SemanticHighlighter(dictionary);
        validator = new UmlModelValidator(dictionary);
        cppLangDef = LanguageMapping.getLanguageDef("CPP");
    }

    @Test
    public void testUMLTypeHighlighting() {
        String code = "std::shared_ptr<Library> lib = factory->createLibrary();";
        List<TextRange> typeRanges = highlighter.getUMLTypeRanges(code, cppLangDef);
        
        assertEquals("Should find exactly 3 UML types (std, shared_ptr, Library)", 3, typeRanges.size());
        
        TextRange range = typeRanges.get(2); // The 3rd match should be Library
        String foundType = code.substring(range.offset, range.offset + range.length);
        assertEquals("Library", foundType);
    }

    @Test
    public void testMethodHighlighting() {
        String code = "lib->printLibrary();";
        List<TextRange> methodRanges = highlighter.getMethodRanges(code, cppLangDef);
        
        assertEquals("Should find exactly 1 method", 1, methodRanges.size());
        
        TextRange range = methodRanges.get(0);
        String foundMethod = code.substring(range.offset, range.offset + range.length);
        assertEquals("printLibrary", foundMethod);
    }

    @Test
    public void testValidationSuccess() {
        String code = "std::shared_ptr<Library> lib;\nlib->printLibrary();";
        List<com.burhankhanzada.opaquebehavioureditor.model.TextRange> errors = validator.validateUMLMemberAccess(code, cppLangDef);
        
        assertTrue("Should have no validation errors", errors.isEmpty());
    }

    @Test
    public void testValidationError() {
        String code = "std::shared_ptr<Library> lib;\nlib->fakeMethod();";
        List<com.burhankhanzada.opaquebehavioureditor.model.TextRange> errors = validator.validateUMLMemberAccess(code, cppLangDef);
        
        assertEquals("Should find exactly 1 validation error", 1, errors.size());
        
        com.burhankhanzada.opaquebehavioureditor.model.TextRange error = errors.get(0);
        String errorMethod = code.substring(error.offset, error.offset + error.length);
        assertEquals("fakeMethod", errorMethod);
    }
}
