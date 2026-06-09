package com.burhankhanzada.opaquebehavioureditor.editor;

public class SnippetLibrary {

    public static class Snippet {
        public final String keyword;
        public final String label;
        public final String template;
        public final String placeholder;
        
        public Snippet(String keyword, String label, String template, String placeholder) {
            this.keyword = keyword;
            this.label = label;
            this.template = template;
            this.placeholder = placeholder;
        }
    }

    public static final Snippet[] SNIPPETS = new Snippet[] {
        new Snippet("create", "⚡ create (Template)", "std::shared_ptr<Type> item = factory->createType();", "Type"),
        new Snippet("for", "⚡ for (Template)", "for(std::shared_ptr<Type> item : *collection) {\n    \n}", "Type"),
        new Snippet("cast", "⚡ cast (Template)", "std::shared_ptr<Type> casted = std::dynamic_pointer_cast<Type>(item);", "Type")
    };
}
