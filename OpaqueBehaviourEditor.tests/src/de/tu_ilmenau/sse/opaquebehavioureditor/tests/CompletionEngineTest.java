package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.ui.CompletionEngine;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;

/**
 * Proposals are ordered by usefulness, not alphabetically: the type's own accessors first, then
 * its other members, then inherited ones, then the reflective API every object carries.
 */
public class CompletionEngineTest {

    private CompletionEngine engine;

    @Before
    public void setUp() {
        ModelDictionary d = new ModelDictionary();

        // Book: own feature "pages", inherits "name" from NamedElement, plus the usual reflective API
        d.addTypeMember("Book", "pages", "int");
        d.addTypeMember("Book", "getPages", "int");
        d.addTypeMember("Book", "setPages", "void");
        d.addTypeMember("Book", "printBook", "void");
        d.addTypeMember("NamedElement", "getName", "String");
        d.addSuperType("Book", "NamedElement");
        d.addTypeMember("aql::Services", "getProperty", "String");
        d.addTypeMember("ecore::EObject", "eGet", "Any");
        d.addSuperType("Book", "ecore::EObject");
        d.addSuperType("ecore::EObject", "aql::Services");

        for (String w : List.of("pages", "getPages", "setPages", "printBook", "getName", "getProperty", "eGet")) {
            d.addAutocompleteWord(w);
        }

        engine = new CompletionEngine(d);
        engine.setLanguage("C++");
    }

    @Test
    public void ownAccessorsComeFirst() {
        List<String> m = engine.findMatches("", true, "Book", "");
        assertEquals("getPages", m.get(0));
        assertEquals("setPages", m.get(1));
        // the bare feature name and the operation rank below the accessors
        assertTrue(m.indexOf("pages") > m.indexOf("setPages"));
        assertTrue(m.indexOf("printBook") > m.indexOf("setPages"));
    }

    @Test
    public void inheritedAccessorsBeatOwnNonAccessors() {
        List<String> m = engine.findMatches("", true, "Book", "");
        assertTrue(m.indexOf("getName") < m.indexOf("pages"));
    }

    @Test
    public void reflectiveApiComesLast() {
        List<String> m = engine.findMatches("", true, "Book", "");
        assertTrue(m.indexOf("getProperty") > m.indexOf("printBook"));
        assertTrue(m.indexOf("eGet") > m.indexOf("printBook"));
    }

    @Test
    public void collectionMethodsAreOrderedByUsage() {
        List<String> m = engine.findMatches("", true, "Bag<Book>", "");
        assertEquals("add", m.get(0));
        assertEquals("size", m.get(1));
        assertTrue(m.indexOf("add") < m.indexOf("max_size"));
    }
}
