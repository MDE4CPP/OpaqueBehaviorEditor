package com.burhankhanzada.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.burhankhanzada.opaquebehavioureditor.editor.text.AutoFormatter;

public class AutoFormatterTest {

    @Test
    public void testBasicFormatting() {
        String code = "int main() {\nint a = 5;\nif(a > 0){\nreturn 1;\n}\nreturn 0;\n}";
        String expected = "int main() {\n    int a = 5;\n    if(a > 0){\n        return 1;\n    }\n    return 0;\n}";
        String formatted = AutoFormatter.format(code, "CPP");
        assertEquals(expected, formatted);
    }

    @Test
    public void testEmptyAndBlankLines() {
        String code = "void test() {\n\n    int x;\n    \n}";
        String expected = "void test() {\n\n    int x;\n\n}";
        String formatted = AutoFormatter.format(code, "CPP");
        assertEquals(expected, formatted);
    }

    @Test
    public void testStringsAndCommentsIgnored() {
        String code = "void test() {\nString s = \"{ { {\";\n// { { {\nint x = 1;\n}";
        String expected = "void test() {\n    String s = \"{ { {\";\n    // { { {\n    int x = 1;\n}";
        String formatted = AutoFormatter.format(code, "CPP");
        assertEquals(expected, formatted);
    }
    
    @Test
    public void testClosingBraceOnSameLine() {
        String code = "void test() {\nif(true) {\nfoo(); } \n}";
        String expected = "void test() {\n    if(true) {\n        foo(); }\n}";
        String formatted = AutoFormatter.format(code, "CPP");
        assertEquals(expected, formatted);
    }
    
    @Test
    public void testClosingBraceAtStartOfLine() {
        String code = "void test() {\nif(true) {\nfoo();\n}\n}";
        String expected = "void test() {\n    if(true) {\n        foo();\n    }\n}";
        String formatted = AutoFormatter.format(code, "CPP");
        assertEquals(expected, formatted);
    }
}
