package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageDef;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageMapping;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelValidator;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.TextRange;

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

    @Test
    public void commentedOutMemberAccessIsNotAnError() {
        String code = "std::shared_ptr<Library> lib;\n"
                    + "//lib->invalidMethod();\n"
                    + "/* lib->alsoInvalid(); */\n";
        assertTrue("Commented-out code must not be validated",
                validator.validateMemberAccess(code, cppLangDef).isEmpty());
    }

    @Test
    public void memberAccessInsideAStringIsNotAnError() {
        String code = "std::shared_ptr<Library> lib;\n"
                    + "std::string s = \"call lib->invalidMethod() here\";\n";
        assertTrue("String literals must not be validated",
                validator.validateMemberAccess(code, cppLangDef).isEmpty());
    }

    @Test
    public void realCodeAfterACommentIsStillValidated() {
        String code = "std::shared_ptr<Library> lib;\n"
                    + "//lib->ignored();\n"
                    + "lib->invalidMethod();\n";
        List<TextRange> errors = validator.validateMemberAccess(code, cppLangDef);
        assertEquals(1, errors.size());
        assertEquals("Method 'invalidMethod' is not defined in class 'Library'", errors.get(0).message);
    }

    @Test
    public void collectionMethodsFollowBag() {
        dictionary.addTypeMember("Library", "getBooks", "Bag<Book>");
        String code = "std::shared_ptr<Library> lib;\n"
                    + "lib->getBooks()->push_back(b);\n"   // real Bag method
                    + "lib->getBooks()->index_of(b);\n"    // real Bag method
                    + "lib->getBooks()->remove(b);\n";     // not in Bag<T>
        List<TextRange> errors = validator.validateMemberAccess(code, cppLangDef);
        assertEquals(1, errors.size());
        assertEquals("Method 'remove' is not defined in class 'Bag<Book>'", errors.get(0).message);
    }

    @Test
    public void subsetIsRecognisedAsACollection() {
        // Subset<> is the second most common collection in generated MDE4CPP code.
        assertTrue(ModelValidator.isCollectionType("Subset<Book, Element>"));
        assertTrue(ModelValidator.isCollectionType("SubsetUnion<Book, Element>"));
        assertTrue(ModelValidator.isCollectionType("Union<Book>"));
        assertTrue(ModelValidator.isCollectionType("Bag<Book>"));
        assertFalse(ModelValidator.isCollectionType("Book"));
    }

    @Test
    public void aLoneUndeclaredIdentifierStatementIsAnError() {
        String code = "std::shared_ptr<Library> lib;\nbookMasadsad;\n";
        List<TextRange> errors = validator.validateStatements(code, cppLangDef);
        assertEquals(1, errors.size());
        assertEquals("'bookMasadsad' is not declared in this body or the model", errors.get(0).message);
        assertEquals(code.indexOf("bookMasadsad"), errors.get(0).offset);
    }

    @Test
    public void aLoneKnownIdentifierStatementStillHasNoEffect() {
        String code = "std::shared_ptr<Library> lib;\nlib;\n";
        List<TextRange> errors = validator.validateStatements(code, cppLangDef);
        assertEquals(1, errors.size());
        assertEquals("Statement 'lib;' has no effect", errors.get(0).message);
    }

    @Test
    public void ordinaryStatementsAndKeywordsAreNotFlagged() {
        String code = "std::shared_ptr<Library> lib;\n"
                    + "lib->print();\n"
                    + "int i = 0;\n"
                    + "for (;;) { break; }\n"
                    + "while (true) { continue; }\n"
                    + "return;\n"
                    + "//bookMasadsad;\n"
                    + "std::string s = \"bookMasadsad;\";\n";
        assertTrue(validator.validateStatements(code, cppLangDef).isEmpty());
    }
}
