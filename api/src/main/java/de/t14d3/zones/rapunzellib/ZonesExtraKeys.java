package de.t14d3.zones.rapunzellib;

import de.t14d3.rapunzellib.objects.RExtraKey;
import de.t14d3.zones.objects.Box;

public final class ZonesExtraKeys {
    private ZonesExtraKeys() {
    }

    public static final RExtraKey<Box> SELECTION = RExtraKey.of("zones", "selection", Box.class);
    public static final RExtraKey<Boolean> SELECTION_CREATING = RExtraKey.of("zones", "selection_creating",
            Boolean.class);
    public static final RExtraKey<String> SELECTION_MODE = RExtraKey.of("zones", "selection_mode", String.class);
    public static final RExtraKey<Long> LAST_ACTIONBAR_NANOS = RExtraKey.of("zones", "last_actionbar_nanos",
            Long.class);
    public static final RExtraKey<ZonesPermissionCache> PERMISSION_CACHE = RExtraKey.of("zones", "permission_cache",
            ZonesPermissionCache.class);
}
