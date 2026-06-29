package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageMapping;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageDef;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.highlighting.HighlightingContext;
import de.tu_ilmenau.sse.opaquebehavioureditor.editor.highlighting.SemanticHighlighter;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.TextRange;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.ModelDictionary;

public class SemanticHighlighterTest {

    private ModelDictionary dictionary;
    private SemanticHighlighter highlighter;
    private LanguageDef cppLangDef;

    @Before
    public void setUp() {
        dictionary = new ModelDictionary();
        // Add some dummy UML types and methods
        dictionary.addClassElement("Library", "dummy", null);
        dictionary.addClassElement("Book", "dummy", null);
        
        dictionary.addTypeMember("Library", "createBook", "uml::Operation");
        dictionary.addTypeMember("Library", "printLibrary", "uml::Operation");

        highlighter = new SemanticHighlighter(dictionary);
        cppLangDef = LanguageMapping.getLanguageDef("CPP");
    }

    @Test
    public void testUMLTypeHighlighting() {
        String code = "std::shared_ptr<Library> lib = factory->createLibrary();";
        HighlightingContext ctx = HighlightingContext.create(code, cppLangDef);
        List<TextRange> typeRanges = highlighter.getUMLTypeRanges(ctx);
        
        assertEquals("Should find exactly 3 UML types (std, shared_ptr, Library)", 3, typeRanges.size());
        
        TextRange range = typeRanges.get(2); // The 3rd match should be Library
        String foundType = code.substring(range.offset, range.offset + range.length);
        assertEquals("Library", foundType);
    }

    @Test
    public void testMethodHighlighting() {
        String code = "lib->printLibrary();";
        HighlightingContext ctx = HighlightingContext.create(code, cppLangDef);
        List<TextRange> methodRanges = highlighter.getMethodRanges(ctx);
        
        assertEquals("Should find exactly 1 method", 1, methodRanges.size());
        
        TextRange range = methodRanges.get(0);
        String foundMethod = code.substring(range.offset, range.offset + range.length);
        assertEquals("printLibrary", foundMethod);
    }

}
