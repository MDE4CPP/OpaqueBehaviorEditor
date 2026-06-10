package com.burhankhanzada.opaquebehavioureditor.editor.highlighting;

import java.util.List;

import com.burhankhanzada.opaquebehavioureditor.editor.text.LanguageDef;
import com.burhankhanzada.opaquebehavioureditor.model.TextRange;

public record HighlightingContext(String text, LanguageDef lang, List<TextRange> ignored) {
    public static HighlightingContext create(String text, LanguageDef lang) {
        return new HighlightingContext(text, lang, SemanticHighlighter.getIgnoredRanges(text));
    }
}
