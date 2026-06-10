package com.burhankhanzada.opaquebehavioureditor.editor.quickfix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.burhankhanzada.opaquebehavioureditor.model.ModelDictionary;
import com.burhankhanzada.opaquebehavioureditor.model.ModelValidator;

/**
 * Provides "Did you mean ...?" suggestions for undefined method errors
 * by computing Levenshtein edit-distance against known type members.
 */
public class QuickFixEngine {

    /** Maximum edit distance to consider a suggestion relevant */
    private static final int MAX_EDIT_DISTANCE = 3;
    /** Maximum number of suggestions to return */
    private static final int MAX_SUGGESTIONS = 5;

    private final ModelDictionary dictionary;

    public QuickFixEngine(ModelDictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Given an undefined method name and the type it was called on,
     * returns a list of suggestions sorted by edit distance (closest first).
     */
    public List<Suggestion> suggest(String undefinedMethod, String typeName) {
        List<Suggestion> suggestions = new ArrayList<>();

        if (undefinedMethod == null || undefinedMethod.isEmpty()) {
            return suggestions;
        }

        // Search in type members of the resolved type
        Map<String, String> members = dictionary.getTypeMembers().get(typeName);
        if (members != null) {
            for (Map.Entry<String, String> entry : members.entrySet()) {
                String candidateName = entry.getKey();
                String returnType = entry.getValue();
                int dist = levenshteinDistance(undefinedMethod.toLowerCase(), candidateName.toLowerCase());
                if (dist <= MAX_EDIT_DISTANCE && dist > 0) {
                    suggestions.add(new Suggestion(candidateName, returnType, dist, typeName));
                }
            }
        }

        // Also check common methods
        for (String cm : ModelValidator.COMMON_METHODS) {
            int dist = levenshteinDistance(undefinedMethod.toLowerCase(), cm.toLowerCase());
            if (dist <= MAX_EDIT_DISTANCE && dist > 0) {
                // Avoid duplicates from typeMembers
                boolean duplicate = false;
                for (Suggestion s : suggestions) {
                    if (s.methodName.equals(cm)) { duplicate = true; break; }
                }
                if (!duplicate) {
                    suggestions.add(new Suggestion(cm, null, dist, typeName));
                }
            }
        }

        // Sort by edit distance (closest match first), then alphabetically
        suggestions.sort((a, b) -> {
            int cmp = Integer.compare(a.editDistance, b.editDistance);
            if (cmp != 0) return cmp;
            return a.methodName.compareTo(b.methodName);
        });

        // Limit results
        if (suggestions.size() > MAX_SUGGESTIONS) {
            return suggestions.subList(0, MAX_SUGGESTIONS);
        }
        return suggestions;
    }

    /**
     * Extracts the type name from a validation error message.
     * Expected format: "Method 'xxx' is not defined in class 'YYY'"
     */
    public static String extractTypeFromErrorMessage(String message) {
        if (message == null) return null;
        int classIdx = message.lastIndexOf("class '");
        if (classIdx < 0) return null;
        int start = classIdx + 7; // length of "class '"
        int end = message.indexOf("'", start);
        if (end < 0) return null;
        return message.substring(start, end);
    }

    /**
     * Extracts the method name from a validation error message.
     * Expected format: "Method 'xxx' is not defined in class 'YYY'"
     */
    public static String extractMethodFromErrorMessage(String message) {
        if (message == null) return null;
        int methodIdx = message.indexOf("Method '");
        if (methodIdx < 0) return null;
        int start = methodIdx + 8; // length of "Method '"
        int end = message.indexOf("'", start);
        if (end < 0) return null;
        return message.substring(start, end);
    }

    /**
     * Computes the Levenshtein edit distance between two strings.
     */
    static int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();

        int[][] dp = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) dp[i][0] = i;
        for (int j = 0; j <= n; j++) dp[0][j] = j;

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[m][n];
    }

    /**
     * A single quick-fix suggestion.
     */
    public static class Suggestion {
        public final String methodName;
        public final String returnType;  // may be null
        public final int editDistance;
        public final String ownerType;

        public Suggestion(String methodName, String returnType, int editDistance, String ownerType) {
            this.methodName = methodName;
            this.returnType = returnType;
            this.editDistance = editDistance;
            this.ownerType = ownerType;
        }

        /**
         * Returns a display label like "getBooks() : Collection<Book>"
         */
        public String getDisplayLabel() {
            String label = methodName + "()";
            if (returnType != null && !returnType.isEmpty()) {
                label += " : " + returnType;
            }
            return label;
        }
    }
}
