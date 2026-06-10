package com.burhankhanzada.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageMapping;
import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;
import com.burhankhanzada.opaquebehavioureditor.model.TextRange;

public class ModelValidatorTest {

    private ModelDictionary dictionary;
    private ModelValidator validator;
    private LanguageDef cppLangDef;

    @Before
    public void setUp() {
        dictionary = new ModelDictionary();
        dictionary.addClassElement("Library", "dummy", null);
        dictionary.addTypeMember("Library", "print", "void");
        validator = new ModelValidator(dictionary);
        cppLangDef = LanguageMapping.getLanguageDef("CPP");
    }

    @Test
    public void testValidMemberAccess() {
        String code = "std::shared_ptr<Library> lib;\nlib->print();";
        List<TextRange> errors = validator.validateMemberAccess(code, cppLangDef);
        assertTrue("Expected no errors", errors.isEmpty());
    }

    @Test
    public void testInvalidMemberAccess() {
        String code = "std::shared_ptr<Library> lib;\nlib->invalidMethod();";
        List<TextRange> errors = validator.validateMemberAccess(code, cppLangDef);
        assertEquals(1, errors.size());
        assertEquals("Method 'invalidMethod' is not defined in class 'Library'", errors.get(0).message);
    }

    @Test
    public void testSyntaxUnmatchedBraces() {
        String code = "int main() { return 0;";
        List<TextRange> errors = validator.validateSyntax(code, cppLangDef);
        assertEquals("Expected an error for unclosed '{'", 1, errors.size());
        assertTrue(errors.get(0).message.contains("Unclosed opening '{'"));
    }

    @Test
    public void testSyntaxUnmatchedClosingBrace() {
        String code = "int main() }";
        List<TextRange> errors = validator.validateSyntax(code, cppLangDef);
        assertEquals("Expected an error for unmatched closing '}'", 1, errors.size());
        assertTrue(errors.get(0).message.contains("Unmatched closing '}'"));
    }
    
    @Test
    public void testSyntaxInCommentsIgnored() {
        String code = "int main() {\n // }\n return 0;\n}";
        List<TextRange> errors = validator.validateSyntax(code, cppLangDef);
        assertTrue("Comments should be ignored for syntax checks", errors.isEmpty());
    }
}
