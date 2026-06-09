package com.burhankhanzada.opaquebehavioureditor.editor.text;

/**
 * Holds all syntax-relevant metadata for a single programming language.
 */
public class LanguageDef {
    public final String name;
    public final String scopeName;
    public final String fileExtension;
    public final String[] keywords;
    public final String[] types;
    public final String singleLineComment;
    public final String multiLineCommentStart;
    public final String multiLineCommentEnd;
    public final String preprocessorPrefix;

    protected LanguageDef(String name, String scopeName, String fileExtension,
                String[] keywords, String[] types,
                String singleLineComment,
                String multiLineCommentStart, String multiLineCommentEnd,
                String preprocessorPrefix) {
        this.name = name;
        this.scopeName = scopeName;
        this.fileExtension = fileExtension;
        this.keywords = keywords;
        this.types = types;
        this.singleLineComment = singleLineComment;
        this.multiLineCommentStart = multiLineCommentStart;
        this.multiLineCommentEnd = multiLineCommentEnd;
        this.preprocessorPrefix = preprocessorPrefix;
    }

    public boolean isPlainText() {
        return name.isEmpty();
    }
}
