package de.tu_ilmenau.sse.opaquebehavioureditor.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageMapping;

/**
 * The language combo is READ_ONLY, so setText only takes effect for a string that is one of its
 * items. A model stores "C++" while the combo is populated from the registered names ("CPP"), so
 * the two must be reconciled - otherwise the combo stays blank, the blank is written back onto the
 * entry, and every validator, which only runs for CPP, silently does nothing.
 */
public class LanguageMappingTest {

    @Test
    public void canonicalNameIsAlwaysOneOfTheComboItems() {
        List<String> items = Arrays.asList(LanguageMapping.getAllLanguageNames());
        for (String spelling : List.of("C++", "cpp", "CPP", "c++17", "Java", "java", "C", "c99")) {
            assertTrue(spelling + " -> " + LanguageMapping.canonicalName(spelling)
                    + " is not selectable in the combo",
                    items.contains(LanguageMapping.canonicalName(spelling)));
        }
    }

    @Test
    public void canonicalNameMapsTheSpellingModelsActuallyUse() {
        assertEquals("CPP", LanguageMapping.canonicalName("C++"));
        assertEquals("CPP", LanguageMapping.canonicalName("cpp"));
        assertEquals("Java", LanguageMapping.canonicalName("java"));
    }

    @Test
    public void everyComboItemResolvesToItself() {
        for (String item : LanguageMapping.getAllLanguageNames()) {
            assertEquals(item, LanguageMapping.getLanguageDef(item).name);
        }
    }

    @Test
    public void aBlankLanguageStaysPlainTextRatherThanBecomingCpp() {
        assertTrue(LanguageMapping.getLanguageDef("").isPlainText());
        assertEquals("", LanguageMapping.canonicalName(""));
    }
}
