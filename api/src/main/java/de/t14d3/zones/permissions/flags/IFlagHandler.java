package de.t14d3.zones.permissions.flags;

import java.util.List;

/**
 * Flag metadata hook: defaults and name suggestions.
 *
 * <p>All permission evaluation and caching lives in the Zones permission engine
 * (not in individual flag handlers).</p>
 */
public interface IFlagHandler {

    default List<String> getValidValues() {
        return List.of();
    }

    default boolean getDefaultValue(FlagContext context) {
        return true;
    }
}
