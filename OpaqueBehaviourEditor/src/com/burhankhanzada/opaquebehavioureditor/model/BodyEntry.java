package com.burhankhanzada.opaquebehavioureditor.model;

import com.burhankhanzada.opaquebehavioureditor.ui.*;
import com.burhankhanzada.opaquebehavioureditor.editor.*;
import com.burhankhanzada.opaquebehavioureditor.model.*;


/**
 * Represents a single opaque behavior body block, which consists of
 * the source code (body) and the language it is written in.
 */
public class BodyEntry {
    public String language;
    public String body;
    
    public BodyEntry(String language, String body) {
        this.language = language;
        this.body = body;
    }
}
