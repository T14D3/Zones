package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.CacheEntry;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.Result;
import de.t14d3.zones.utils.DebugLoggerManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class UniversalFlag implements FlagInterface {
    private final CacheUtils cacheUtils;
    private List<String> validValues;
    private boolean defaultValue;

    private static final String UNIVERSAL = "universal";

    public UniversalFlag(List<String> validValues, boolean defaultValue) {
        this.validValues = validValues;
        this.defaultValue = defaultValue;
        this.cacheUtils = CacheUtils.getInstance();
    }

    public Result evaluate(Region region, String permission, String type, Object... optionals) {
        if (cacheUtils.permissionCache.containsKey(UNIVERSAL)) {
            ConcurrentLinkedQueue<CacheEntry> entries = cacheUtils.permissionCache.get(UNIVERSAL);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(permission, type, region.getKey())) {
                    DebugLoggerManager.Logger()
                            .log(DebugLoggerManager.CACHE_HIT_PERM, permission, region.getKey(), type,
                                    DebugLoggerManager.UNI_CHECK);
                    return entry.result;
                }
            }
        }
        Result result = Result.UNDEFINED;
        Map<String, String> universalPerms = region.getMembers().get(UNIVERSAL);
        if (universalPerms != null) {
            String value = universalPerms.get(permission.toLowerCase());
            if (value != null) {
                value = value.toLowerCase().trim();
                for (String part : value.split(",")) {
                    result = isAllowed(part, type, result);
                }
            }
        }
        cacheUtils.permissionCache.computeIfAbsent(UNIVERSAL, k -> new ConcurrentLinkedQueue<>())
                .add(new CacheEntry(permission, type, region.getKey(), result));
        DebugLoggerManager.Logger().log(DebugLoggerManager.CACHE_MISS_PERM, permission, region.getKey(), type,
                DebugLoggerManager.UNI_CHECK);
        return result;
    }

    private static Result isAllowed(String perm, String type, Result result) {
        perm = perm.toLowerCase();
        type = type.toLowerCase();
        if (perm.equals("true") || perm.equals("*")) {
            result = Result.TRUE;
        } else if (perm.equals("false") || perm.equals("!*")) {
            result = Result.FALSE;
        } else if (perm.equals(type)) {
            result = Result.TRUE;
        } else if (perm.equals("!" + type)) {
            result = Result.FALSE;
        }
        return result;
    }

    public boolean getDefaultValue(Object... optional) {
        return defaultValue;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public List<String> getValidValues() {
        return validValues;
    }
}
