package de.t14d3.zones.permissions;

import de.t14d3.zones.objects.Result;

/**
 * A resolved permission decision with a specificity score.
 *
 * <p>Higher specificity wins. If two matches have equal specificity, deny wins.</p>
 */
public record Decision(Result result, int specificity) {
    public static Decision undefined() {
        return new Decision(Result.UNDEFINED, -1);
    }

    public static Decision global(Result result) {
        return new Decision(result, 0);
    }
}

