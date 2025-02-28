package de.t14d3.zones.objects;

/**
 * The result of a permission check
 * TRUE/FALSE overwrite UNDEFINED
 */
public enum Result {
    TRUE, FALSE, UNDEFINED;

    public static Result valueOf(boolean value) {
        return value ? TRUE : FALSE;
    }
}
