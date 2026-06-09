package com.burhankhanzada.opaquebehavioureditor;

/**
 * Centralized string constants for the Opaque Behaviour Editor UI and popups.
 */
public final class StringConstants {
    
    private StringConstants() {
        // Prevent instantiation
    }

    // --- Dialog Titles & Messages ---
    public static final String DIALOG_TITLE = "Edit OpaqueBehaviour Body";
    public static final String DIALOG_MSG_EDITING = "Editing body of: ";
    public static final String DIALOG_MSG_DEFAULT = "Edit body entries of the selected OpaqueBehaviour";

    // --- Labels ---
    public static final String LBL_BODY_ENTRIES = "Body entries:";
    public static final String LBL_LANGUAGE = "Language:";
    public static final String LBL_TRANSLATE_TO = "  Translate to:";
    public static final String LBL_BODY_CODE = "Body code:";

    // --- Buttons ---
    public static final String BTN_ADD = "Add";
    public static final String BTN_REMOVE = "Remove";
    public static final String BTN_UP = "Up";
    public static final String BTN_DOWN = "Down";
    public static final String BTN_FORMAT_CODE = "Format Code";
    public static final String BTN_TRANSLATE = "Translate";

    // --- Popups (Handler) ---
    public static final String POPUP_TITLE_ERR = "Body Editor Error";
    public static final String POPUP_MSG_ERR_PREFIX = "An error occurred:\n";
    
    public static final String POPUP_TITLE_INFO = "Body Editor";
    public static final String POPUP_MSG_NO_SELECTION = "No element selected.";
    public static final String POPUP_MSG_NOT_STRING = "The selected Map Entry value is not a String.\nKey is: ";
    public static final String POPUP_MSG_INVALID_SELECTION = "Selected element is neither an OpaqueBehavior nor a valid Map Entry.\nType: ";

    // --- Fallbacks ---
    public static final String FALLBACK_ANNOTATION_NAME = "Ecore Annotation Body";
    public static final String FORMAT_ANNOTATION_NAME = "Ecore Annotation (%s)";
}
