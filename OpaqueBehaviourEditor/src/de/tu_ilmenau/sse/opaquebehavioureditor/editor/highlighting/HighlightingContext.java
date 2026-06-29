package de.tu_ilmenau.sse.opaquebehavioureditor.editor.highlighting;

import java.util.List;

import de.tu_ilmenau.sse.opaquebehavioureditor.editor.text.LanguageDef;
import de.tu_ilmenau.sse.opaquebehavioureditor.model.TextRange;

public record HighlightingContext(String text, LanguageDef lang, List<TextRange> ignored) {
    public static HighlightingContext create(String text, LanguageDef lang) {
        return new HighlightingContext(text, lang, SemanticHighlighter.getIgnoredRanges(text));
    }
}
