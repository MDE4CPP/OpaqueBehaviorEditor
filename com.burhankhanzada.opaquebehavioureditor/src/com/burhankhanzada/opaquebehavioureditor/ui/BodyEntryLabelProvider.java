package com.burhankhanzada.opaquebehavioureditor.ui;

import com.burhankhanzada.opaquebehavioureditor.ui.*;
import com.burhankhanzada.opaquebehavioureditor.editor.*;
import com.burhankhanzada.opaquebehavioureditor.model.*;


import java.util.List;
import org.eclipse.jface.viewers.LabelProvider;

public class BodyEntryLabelProvider extends LabelProvider {
    
    private final List<BodyEntry> entries;

    public BodyEntryLabelProvider(List<BodyEntry> entries) {
        this.entries = entries;
    }

    @Override
    public String getText(Object element) {
        if (!(element instanceof BodyEntry entry)) return super.getText(element);
        int idx = entries.indexOf(entry);
        String preview = entry.body.replace('\n', ' ').replace('\r', ' ').strip();
        if (preview.length() > 60) preview = preview.substring(0, 57) + "...";
        String lang = entry.language.isBlank() ? "(no language)" : entry.language;
        return String.format("%d: [%s]  %s", idx, lang, preview);
    }
}
