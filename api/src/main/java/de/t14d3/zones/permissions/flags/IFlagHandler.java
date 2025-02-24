package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Result;
import de.t14d3.zones.permissions.CacheEntry;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.utils.DebugLoggerManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static de.t14d3.zones.permissions.PermissionManager.isAllowed;

public interface IFlagHandler {
    String UNIVERSAL = "universal";
    CacheUtils cacheUtils = CacheUtils.getInstance();
    FlagTypes flagType = FlagTypes.OTHER;

    default List<String> getValidValues() {
        return List.of();
    }

    default boolean getDefaultValue(Object... optional) {
        return true;
    }

    default Result evaluate(Region region, String permission, String type, Object... optionals) {
        return evaluate(region, UNIVERSAL, permission, type, optionals);
    }

    default Result evaluate(Region region, String who, String permission, String type, Object... optionals) {
        permission = permission.toLowerCase();
        if (cacheUtils.permissionCache.containsKey(who)) {
            ConcurrentLinkedQueue<CacheEntry> entries = cacheUtils.permissionCache.get(who);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(permission, type, region.getKey())) {
                    DebugLoggerManager.Logger()
                            .log(DebugLoggerManager.CACHE_HIT_PERM, permission, who, region.getKey(), type);
                    return entry.result;
                }
            }
        }


        Result result = Result.UNDEFINED; // Initialize result as null

        // Check if player is an admin, but only if not checking
        // for "role" permission (StackOverflowException prevention)
        if (!permission.equalsIgnoreCase("role")) {
            if (isAdmin(who, region)) {
                result = Result.TRUE;
            }
        }

        // Retrieve the permissions for the player in the specified region
        Map<String, Map<String, String>> members = region.getMembers();

        // Get the permissions for the player
        Map<String, String> permissions = members.get(who);
        if (permissions == null) {
            return Result.UNDEFINED; // Return null if no permission was set
        }
        String value = permissions.get(permission);

        // If no value found, check parent region and group permissions
        if (value == null) {
            if (region.getParent() != null) {
                return evaluate(region.getParentRegion(), who, permission, type);
            }
            if (permissions.containsKey("group")) {
                if (who.startsWith("+group-") && !Zones.getInstance().getConfig()
                        .getBoolean("allow-group-recursion", false)) {
                    Zones.getInstance().getLogger()
                            .error("Recursive group permissions detected!! Groups are not allowed to contain other groups!");
                    Zones.getInstance().getLogger().error("Group '" + who.substring(
                            7) + "' contains 'group' permission entry in region '" + region.getKey() + "'");
                    Zones.getInstance().getLogger()
                            .error("If you are 100% sure this is fine, add 'allow-group-recursion: true' to your config.yml");
                    return Result.FALSE;
                }
                for (String group : permissions.get("group").split(",")) {
                    Result temp = evaluate(region, "+group-" + group, permission, type);
                    if (temp.equals(Result.TRUE) || temp.equals(Result.FALSE)) {
                        result = temp;
                    }
                }
            }
            // Nothing found
            else {
                return result; // Return initial result
            }
        }

        if (value != null) {
            value = value.toLowerCase().trim();
            for (String permittedValue : value.split(",")) {
                result = isAllowed(permittedValue, type, result);
            }
        }
        cacheUtils.permissionCache.computeIfAbsent(who, k -> new ConcurrentLinkedQueue<>())
                .add(new CacheEntry(permission, type, region.getKey(), result));
        DebugLoggerManager.Logger().log(DebugLoggerManager.CACHE_MISS_PERM, permission, who, region.getKey(), type);
        return result; // Return null if no permission was set
    }

    private boolean isAdmin(String who, Region region) {
        return evaluate(region, who, "role", "owner").equals(Result.TRUE) || evaluate(region, who, "role",
                "admin").equals(Result.TRUE);
    }

    enum FlagTypes {
        PLAYER,
        UNIVERSAL,
        OTHER
    }

}
