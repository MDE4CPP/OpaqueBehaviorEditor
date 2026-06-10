package com.burhankhanzada.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CToCppTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CToJavaTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CppToCTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.CppToJavaTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.JavaToCTranslator;
import com.burhankhanzada.opaquebehavioureditor.editor.text.translators.JavaToCppTranslator;

public class TranslatorsTest {

    @Test
    public void testJavaToCppTranslator() {
        JavaToCppTranslator translator = new JavaToCppTranslator();
        
        String input = "Library lib = factory.createLibrary();\nlib.print();";
        String expected = "std::shared_ptr<Library> lib = factory->createLibrary();\nlib->print();";
        assertEquals(expected, translator.translate(input));
    }

    @Test
    public void testCppToJavaTranslator() {
        CppToJavaTranslator translator = new CppToJavaTranslator();
        
        String input = "std::shared_ptr<Library> lib = factory->createLibrary();\nlib->print();";
        String expected = "Library lib = factory.createLibrary();\nlib.print();";
        assertEquals(expected, translator.translate(input));
    }

    @Test
    public void testCToJavaTranslator() {
        CToJavaTranslator translator = new CToJavaTranslator();
        
        String input = "Library* lib = createLibrary();";
        String expected = "Library lib = createLibrary();"; // Assuming simple translation for now
        assertEquals(expected, translator.translate(input));
    }

    @Test
    public void testCToCppTranslator() {
        CToCppTranslator translator = new CToCppTranslator();
        
        String input = "printf(\"Hello\");";
        assertNotNull(translator.translate(input));
    }

    @Test
    public void testCppToCTranslator() {
        CppToCTranslator translator = new CppToCTranslator();
        
        String input = "std::cout << \"Hello\" << std::endl;";
        assertNotNull(translator.translate(input));
    }

    @Test
    public void testJavaToCTranslator() {
        JavaToCTranslator translator = new JavaToCTranslator();
        
        String input = "System.out.println(\"Hello\");";
        assertNotNull(translator.translate(input));
    }
}
