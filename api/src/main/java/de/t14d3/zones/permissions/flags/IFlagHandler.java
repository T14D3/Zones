package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.RegionFlagEntry;
import de.t14d3.zones.objects.Result;
import de.t14d3.zones.permissions.CacheEntry;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.utils.DebugLoggerManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

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


        final AtomicReference<Result> result = new AtomicReference<>(Result.UNDEFINED); // Initialize result as null

        // Check if player is an admin, but only if not checking
        // for "role" permission (StackOverflowException prevention)
        if (!permission.equalsIgnoreCase("role")) {
            if (isAdmin(who, region)) {
                result.set(Result.TRUE);
            }
        }

        // Retrieve the permissions for the player in the specified region
        Map<String, List<RegionFlagEntry>> members = region.getMembers();

        // Get the permissions for the player
        List<RegionFlagEntry> permissions = members.get(who);
        if (permissions == null) {
            return result.get(); // Return null if no permission was set
        }

        String finalPermission = permission.toLowerCase();
        permissions.stream().filter(entry -> entry.getFlag().name().equalsIgnoreCase(finalPermission)).findFirst()
                .ifPresentOrElse(entry -> {
                    int priority = 0;
                    for (RegionFlagEntry.FlagValue value : entry.getValues()) {
                        if (value.getValue().equalsIgnoreCase(type) && priority < 2) {
                            priority = 2;
                            result.set(value.isInverted() ? Result.FALSE : Result.TRUE);
                        } else if (value.getValue().equalsIgnoreCase("true") && priority < 1) {
                            priority = 1;
                            result.set(Result.TRUE);
                        } else if (value.getValue().equalsIgnoreCase("false") && priority < 1) {
                            priority = 1;
                            result.set(Result.FALSE);
                        }
                    }
                    if (priority == 0) {
                        result.set(Result.FALSE);
                    }

                }, () -> {
                    if (region.getParent() != null) {
                        result.set(evaluate(region.getParentRegion(), who, finalPermission, type));
                        return;
                    }
                    if (permissions.stream().anyMatch(entry -> entry.getFlag().name().equalsIgnoreCase("group"))) {
                        if (who.startsWith("+group-") && !Zones.getInstance().getConfig()
                                .getBoolean("allow-group-recursion", false)) {
                            Zones.getInstance().getLogger()
                                    .error("Recursive group permissions detected!! Groups are not allowed to contain other groups!");
                            Zones.getInstance().getLogger()
                                    .error("Group '{}' contains 'group' permission entry in region '{}'", who.substring(
                                            7), region.getKey());
                            Zones.getInstance().getLogger()
                                    .error("If you are 100% sure this is fine, add 'allow-group-recursion: true' to your config.yml");
                            result.set(Result.FALSE);
                            return;
                        }
                        RegionFlagEntry groups = permissions.stream()
                                .filter(entry -> entry.getFlag().name().equalsIgnoreCase("group")).findFirst().get();
                        for (String group : groups.getValues().stream().map(RegionFlagEntry.FlagValue::getValue)
                                .toList()) {
                            Result temp = evaluate(region, "+group-" + group, finalPermission, type);
                            if (temp.equals(Result.TRUE) || temp.equals(Result.FALSE)) {
                                result.set(temp);
                            }
                        }
                    }

                });
        cacheUtils.permissionCache.computeIfAbsent(who, k -> new ConcurrentLinkedQueue<>())
                .add(new CacheEntry(permission, type, region.getKey(), result.get()));
        DebugLoggerManager.Logger().log(DebugLoggerManager.CACHE_MISS_PERM, permission, who, region.getKey(), type);
        return result.get(); // Return null if no permission was set
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
