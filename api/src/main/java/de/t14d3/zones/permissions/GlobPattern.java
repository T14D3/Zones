package de.t14d3.zones.permissions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Simple '*' glob matcher (not regex).
 */
public final class GlobPattern {
    private final String raw;
    private final boolean startsWithWildcard;
    private final boolean endsWithWildcard;
    private final String[] parts;
    private final int specificity;

    private GlobPattern(String raw, boolean startsWithWildcard, boolean endsWithWildcard, String[] parts, int specificity) {
        this.raw = raw;
        this.startsWithWildcard = startsWithWildcard;
        this.endsWithWildcard = endsWithWildcard;
        this.parts = parts;
        this.specificity = specificity;
    }

    public static GlobPattern compile(String pattern) {
        if (pattern == null) pattern = "";
        String p = pattern.trim().toLowerCase(Locale.ROOT);
        boolean starts = p.startsWith("*");
        boolean ends = p.endsWith("*");

        // Split on '*' and drop empty segments.
        List<String> segments = new ArrayList<>();
        int last = 0;
        for (int i = 0; i < p.length(); i++) {
            if (p.charAt(i) != '*') continue;
            if (i > last) {
                String seg = p.substring(last, i);
                if (!seg.isEmpty()) segments.add(seg);
            }
            last = i + 1;
        }
        if (last < p.length()) {
            String seg = p.substring(last);
            if (!seg.isEmpty()) segments.add(seg);
        }

        int spec = 0;
        for (String s : segments) spec += s.length();
        return new GlobPattern(p, starts, ends, segments.toArray(new String[0]), spec);
    }

    public int specificity() {
        return specificity;
    }

    public boolean matches(String input) {
        if (input == null) return false;
        String in = input.toLowerCase(Locale.ROOT);
        if (raw.equals("*")) return true;
        if (parts.length == 0) return raw.equals(in);

        int idx = 0;
        int partIndex = 0;

        if (!startsWithWildcard) {
            String first = parts[0];
            if (!in.startsWith(first)) return false;
            idx = first.length();
            partIndex = 1;
        }

        int lastPart = parts.length - 1;
        if (!endsWithWildcard) {
            // Reserve the last part for the suffix check.
            lastPart -= 1;
        }

        for (int i = partIndex; i <= lastPart; i++) {
            String part = parts[i];
            int found = in.indexOf(part, idx);
            if (found < 0) return false;
            idx = found + part.length();
        }

        if (!endsWithWildcard) {
            String suffix = parts[parts.length - 1];
            int suffixIdx = in.lastIndexOf(suffix);
            return suffixIdx >= idx && suffixIdx + suffix.length() == in.length();
        }

        return true;
    }
}
