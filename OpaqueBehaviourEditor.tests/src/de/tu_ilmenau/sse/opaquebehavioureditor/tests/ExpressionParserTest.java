package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.ExpressionParser;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;

public class ExpressionParserTest {

    private ModelDictionary dictionary;

    @Before
    public void setUp() {
        dictionary = new ModelDictionary();
        dictionary.addClassElement("Library", "dummy", null);
        dictionary.addClassElement("Book", "dummy", null);
        dictionary.addTypeMember("Library", "createBook", "uml::Operation");
        dictionary.addTypeMember("Library", "books", "Bag<Book>");
    }

    @Test
    public void testResolveVariableType() {
        String code = "std::shared_ptr<Library> myLib = factory->createLibrary();\nmyLib->";
        String type = ExpressionParser.resolveContextTypeFromText(code, dictionary, code);
        assertEquals("Should extract type from variable declaration", "Library", type);
    }

    @Test
    public void testResolveMemberAccessChain() {
        // e.g. lib->books->
        String code = "std::shared_ptr<Library> myLib;\nmyLib->books->";
        String type = ExpressionParser.resolveContextTypeFromText(code, dictionary, code);
        // It currently extracts the raw type of the last part
        assertEquals("Should extract member return type", "Bag<Book>", type);
    }

    @Test
    public void testUnresolvableVariable() {
        String code = "unknownVar->";
        String type = ExpressionParser.resolveContextTypeFromText(code, dictionary, code);
        assertNull("Should return null for unknown variables", type);
    }

    @Test
    public void testContextVariablesResolveWithoutADeclaration() {
        // `factory` and operation parameters are injected by the editor, never declared in the body
        dictionary.addContextVariable("factory", "factory");
        dictionary.addContextVariable("newAuthor", "Author");

        String code = "factory->";
        assertEquals("factory", ExpressionParser.resolveContextTypeFromText(code, dictionary, code));
        String param = "newAuthor->";
        assertEquals("Author", ExpressionParser.resolveContextTypeFromText(param, dictionary, param));
    }

    @Test
    public void testDeclarationAfterTheCaretIsIgnored() {
        // A variable declared later cannot describe an earlier use.
        String code = "myLib->\nstd::shared_ptr<Book> myLib = factory->createBook();\n";
        assertNull(ExpressionParser.resolveContextTypeFromText("myLib->", dictionary, code));
    }

    @Test
    public void testNearestPrecedingDeclarationWins() {
        String code = "std::shared_ptr<Book> x = factory->createBook();\n"
                    + "std::shared_ptr<Library> x = factory->createLibrary();\n"
                    + "x->";
        assertEquals("Library", ExpressionParser.resolveContextTypeFromText(code, dictionary, code));
    }

    @Test
    public void testCommentedOutDeclarationIsIgnored() {
        String code = "//std::shared_ptr<Book> x;\nstd::shared_ptr<Library> x;\nx->";
        assertEquals("Library", ExpressionParser.resolveContextTypeFromText(code, dictionary, code));
    }

    @Test
    public void testDeclaredVariables() {
        String code = "std::shared_ptr<LibraryModel> lm = factory->createLibraryModel();\n"
                    + "std::string bookname = \"Magritte\";\n"
                    + "int pages = 448;\n"
                    + "//std::shared_ptr<Book> commented = nullptr;\n"
                    + "lm->printLibrary();\n"
                    + "return lm;\n";
        java.util.Set<String> vars = ExpressionParser.declaredVariables(code, code.length());
        assertTrue(vars.contains("lm"));
        assertTrue(vars.contains("bookname"));
        assertTrue(vars.contains("pages"));
        assertFalse("declarations in comments are not variables", vars.contains("commented"));
        assertFalse("`return lm;` is not a declaration", vars.contains("printLibrary"));
    }
}
