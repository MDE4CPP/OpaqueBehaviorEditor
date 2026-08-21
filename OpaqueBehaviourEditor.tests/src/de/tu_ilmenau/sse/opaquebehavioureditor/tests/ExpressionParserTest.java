package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
    public void testFactoryResolvesToTheSyntheticFactoryType() {
        // `factory` is injected by the editor, it is never declared in the body text.
        String code = "factory->";
        assertEquals("factory", ExpressionParser.resolveContextTypeFromText(code, dictionary, code));
    }
}
