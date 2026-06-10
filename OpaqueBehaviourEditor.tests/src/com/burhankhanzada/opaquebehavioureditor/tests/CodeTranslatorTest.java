package com.burhankhanzada.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.burhankhanzada.opaquebehavioureditor.editor.text.CodeTranslator;

public class CodeTranslatorTest {

    @Test
    public void testTranslateCppToJava() {
        String cppCode = "std::shared_ptr<Library> lib = factory->createLibrary();\nlib->print();";
        String expectedJava = "Library lib = factory.createLibrary();\nlib.print();";
        String translated = CodeTranslator.translate(cppCode, "CPP", "Java");
        assertEquals(expectedJava, translated);
    }

    @Test
    public void testTranslateJavaToCpp() {
        String javaCode = "Library lib = factory.createLibrary();\nlib.print();";
        String expectedCpp = "std::shared_ptr<Library> lib = factory->createLibrary();\nlib->print();";
        String translated = CodeTranslator.translate(javaCode, "Java", "CPP");
        assertEquals(expectedCpp, translated);
    }

    @Test
    public void testTranslateSameLanguage() {
        String code = "int a = 5;";
        String translated = CodeTranslator.translate(code, "CPP", "CPP");
        assertEquals("Translating to the same language should return the original code", code, translated);
    }
    
    @Test
    public void testUnsupportedTranslation() {
        String code = "int a = 5;";
        String translated = CodeTranslator.translate(code, "UnknownSource", "UnknownTarget");
        assertEquals("Unsupported translation should return the original code", code, translated);
    }
}
