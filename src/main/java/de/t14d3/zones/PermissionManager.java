package de.t14d3.zones;

import de.t14d3.zones.utils.Flag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PermissionManager {

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> interactionCache = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> permissionCache = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>>();
    private RegionManager regionManager;

    public PermissionManager() {
    }

    private static Result isAllowed(String perm, String type, Result result) {
        if (perm.equalsIgnoreCase("true") || perm.equals("*")) {
            result = Result.TRUE;
        } else if (perm.equalsIgnoreCase("false") || perm.equals("!*")) {
            result = Result.FALSE;
        } else if (perm.equalsIgnoreCase(type)) {
            result = Result.TRUE;
        } else if (perm.equalsIgnoreCase("!" + type)) {
            result = Result.FALSE;
        }
        return result;
    }

    // Setter for RegionManager to avoid circular dependency
    public void setRegionManager(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public boolean checkAction(Location location, UUID playerUUID, Flag action, String type, Object... extra) {
        return checkAction(location, playerUUID.toString(), action, type, extra);
    }

    /**
     * Checks if a player can interact with a region.
     *
     * @param location The location of the interaction.
     * @param who      The UUID of the player.
     * @param action   The action the player wants to perform.
     * @param type     The type of the block or entity the interaction happened with.
     * @param extra    Additional, optional information, for example a spawn reason.
     * @return True if the player can interact with the region, false otherwise.
     */
    public boolean checkAction(Location location, String who, Flag action, String type, Object... extra) {
        boolean nonplayer = who.equalsIgnoreCase("+universal") || extra.length != 0 && (boolean) extra[0];
        if (nonplayer) {
            return checkAction(location, action, type, extra);
        }
        List<Region> regions = regionManager.getRegionsAt(location);
        if (!regions.isEmpty()) {
            Result result = Result.UNDEFINED;
            int priority = Integer.MIN_VALUE;

            for (Region region : regions) {

                // Only check regions with a higher priority than the current value
                if (region.getPriority() > priority) {
                    Result hasPermission = hasPermission(who, action.name(), type, region);
                    if (!hasPermission.equals(Result.UNDEFINED)) {
                        result = hasPermission;
                        priority = region.getPriority();
                        continue;
                    }
                }
                // If same priority, both have to be true, otherwise will assume false
                else if (region.getPriority() == priority) {
                    Result hasPermission = hasPermission(who, action.name(), type, region);
                    if (hasPermission.equals(Result.FALSE) || result.equals(Result.FALSE)) {
                        result = Result.FALSE;
                        priority = region.getPriority();
                        continue;
                    }
                }
            }

            if (result.equals(Result.UNDEFINED)) {
                result = Result.valueOf(action.getDefaultValue());
            }
            return result.equals(Result.TRUE);

        } else {
            // If no region found, check for player or universal type

            // Else, test player for bypass permission
            Player player = null;
            try {
                player = Bukkit.getPlayer(UUID.fromString(who));
                if (player != null && player.hasPermission("zones.bypass.unclaimed")) {
                    return true;
                }
            } catch (IllegalArgumentException ignored) {
            }
            return false;
        }
    }

    /**
     * Checks if a universal/non-player action is allowed at a location.
     * This method bypasses player-specific checks for efficiency.
     *
     * @param location The location of the interaction
     * @param action   The action being performed
     * @param type     The type of block/entity involved
     * @param extra    Additional context (used for cache control)
     * @return true if the action is allowed, false otherwise
     */
    public boolean checkAction(Location location, Flag action, String type, Object... extra) {
        String who = "+universal";
        boolean base = extra == null || extra.length == 0;

        // Check interaction cache
        if (base && interactionCache.containsKey(who)) {
            ConcurrentLinkedQueue<CacheEntry> entries = interactionCache.get(who);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(location, action.name(), type)) {
                    return entry.result.equals(Result.TRUE);
                }
            }
        }

        List<Region> regions = regionManager.getRegionsAt(location);
        if (!regions.isEmpty()) {
            Result finalResult = Result.UNDEFINED;
            int highestPriority = Integer.MIN_VALUE;

            for (Region region : regions) {
                if (region.getPriority() > highestPriority) {
                    Result regionResult = hasPermission(region, action.name(), type);
                    if (regionResult != Result.UNDEFINED) {
                        finalResult = regionResult;
                        highestPriority = region.getPriority();
                    }
                } else if (region.getPriority() == highestPriority) {
                    Result regionResult = hasPermission(region, action.name(), type);
                    if (regionResult == Result.FALSE || finalResult == Result.FALSE) {
                        finalResult = Result.FALSE;
                        highestPriority = region.getPriority();
                    }
                }
            }

            // Fallback to default if no regions defined the permission
            if (finalResult == Result.UNDEFINED) {
                finalResult = Result.valueOf(action.getDefaultValue());
            }

            // Update cache if needed
            if (base) {
                interactionCache.computeIfAbsent(who, k -> new ConcurrentLinkedQueue<>())
                        .add(new CacheEntry(location, action.name(), type, finalResult));
            }

            return finalResult == Result.TRUE;
        } else {
            // No regions at location - use default value
            return action.getDefaultValue();
        }
    }

    /**
     * Invalidates the interaction cache for a player.
     * Should be called e.g. when a player logs out.
     *
     * @param target The player to invalidate the cache for.
     */
    public void invalidateInteractionCache(UUID target) {
        interactionCache.remove(target.toString());
    }

    /**
     * Invalidates the interaction cache for all players.
     * Should be called e.g. when the plugin is reloaded
     * or when a region area is changed.
     */
    public void invalidateInteractionCaches() {
        interactionCache.clear();
    }

    /**
     * Invalidates the interaction cache for a chunk.
     * Should be called e.g. when a chunk is unloaded.
     */
    public void invalidateInteractionCacheForChunk(int chunkX, int chunkZ, String world) {
        Bukkit.getScheduler().runTaskAsynchronously(Zones.getInstance(), () -> {
            synchronized (interactionCache) {
                interactionCache.forEach((uuid, cacheEntries) -> {
                    for (CacheEntry entry : cacheEntries) {
                        Location location = (Location) entry.getFlag();
                        if (!world.equals(location.getWorld().getName())) {
                            continue;
                        }
                        int locX = location.getBlockX() >> 4;
                        int locZ = location.getBlockZ() >> 4;
                        if (chunkX == locX && chunkZ == locZ) {
                            interactionCache.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>()).remove(entry);
                        }
                    }
                });
            }
        });
    }

    /**
     * Invalidates the flag/permission cache for a player.
     * Should be called when a region's permissions are changed
     *
     * @param target The target player/group to invalidate the cache for.
     */
    public void invalidateCache(String target) {
        permissionCache.remove(target);
    }

    /**
     * Invalidates the flag/permission cache for all players.
     * Should be called when the plugin is reloaded
     * or when a region area is changed.
     */
    public void invalidateCaches() {
        permissionCache.clear();
    }

    /**
     * hasPermission(String, String, String, Region) overload / bool
     *
     * @param uuid       The UUID of the player whose permission is being checked.
     * @param permission The permission being checked (e.g., "break", "place").
     * @param type       The type of object the permission applies to (e.g., "GRASS_BLOCK").
     * @param region     The region in which the permission is being checked.
     * @param extra      Additional, optional information, for example a spawn reason.
     * @return true if the player has the specified permission for the type, false otherwise.
     * @see #hasPermission(String, String, String, Region, Object...)
     */
    public boolean hasPermission(UUID uuid, String permission, String type, Region region, Object... extra) {
        return hasPermission(uuid.toString(), permission, type, region, extra).equals(Result.TRUE);
    }

    /**
     * Checks if a player has a specific permission for a given type in the provided region.
     *
     * @param who        Who to check the permission for
     * @param permission The permission being checked (e.g., "break", "place").
     * @param type       The type of object the permission applies to (e.g., "GRASS_BLOCK").
     * @param region     The region in which the permission is being checked.
     * @param extra      Additional, optional information, for example a spawn reason.
     * @return {@link Result}
     */
    public Result hasPermission(String who, String permission, String type, Region region, Object... extra) {
        if (permissionCache.containsKey(who)) {
            ConcurrentLinkedQueue<CacheEntry> entries = permissionCache.get(who);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(permission, type, region.getKey())) {
                    return entry.result;
                }
            }
        }
        Result result = Result.UNDEFINED; // Initialize result as null
        result = calculatePermission(who, permission, type, region, extra);
        permissionCache.computeIfAbsent(who, k -> new ConcurrentLinkedQueue<>())
                .add(new CacheEntry(permission, type, region.getKey(), result));
        return result;
    }

    /**
     * Separate method for checking permissions specifically for "+universal" flags,
     * since these can be called quite frequently and do not need some of the extra
     * complexity of the main checkAction method (Groups, recursion checking, etc.)
     *
     * @param region     Region to check
     * @param permission Permission to check
     * @param type       Type of object to check
     * @return Result of the permission check
     */
    private Result hasPermission(Region region, String permission, String type) {
        String who = "+universal";
        // Check cache first
        if (permissionCache.containsKey(who)) {
            ConcurrentLinkedQueue<CacheEntry> entries = permissionCache.get(who);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(permission, type, region.getKey())) {
                    return entry.result;
                }
            }
        }
        Result result = Result.UNDEFINED;
        Map<String, String> universalPerms = region.getMembers().get(who);
        if (universalPerms != null) {
            String value = universalPerms.get(permission.toLowerCase());
            if (value != null) {
                value = value.toLowerCase().trim();
                for (String part : value.split(",")) {
                    result = isAllowed(part, type, result);
                }
            }
        }

        // If not found in current region, check parent
        if (result == Result.UNDEFINED && region.getParent() != null) {
            Region parent = region.getParentRegion(regionManager);
            result = hasPermission(parent, permission, type);
        }

        // Cache the result if it's not UNDEFINED
        if (result != Result.UNDEFINED) {
            permissionCache.computeIfAbsent(who, k -> new ConcurrentLinkedQueue<>())
                    .add(new CacheEntry(permission, type, region.getKey(), result));
        }

        return result;
    }

    private Result calculatePermission(String who, String permission, String type, Region region, Object... extra) {
        permission = permission.toLowerCase();

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
                return hasPermission(who, permission, type, region.getParentRegion(this.regionManager));
            }
            if (permissions.containsKey("group")) {
                if (who.startsWith("+group-") && !Zones.getInstance().getConfig()
                        .getBoolean("allow-group-recursion", false)) {
                    Zones.getInstance().getLogger()
                            .severe("Recursive group permissions detected!! Groups are not allowed to contain other groups!");
                    Zones.getInstance().getLogger().severe("Group '" + who.substring(
                            7) + "' contains 'group' permission entry in region '" + region.getKey() + "'");
                    Zones.getInstance().getLogger()
                            .severe("If you are 100% sure this is fine, add 'allow-group-recursion: true' to your config.yml");
                    return Result.FALSE;
                }
                for (String group : permissions.get("group").split(",")) {
                    Result temp = hasPermission("+group-" + group, permission, type, region);
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

        return result; // Return null if no permission was set
    }

    public boolean isAdmin(String who, Region region) {
        return hasPermission(who, "role", "owner", region).equals(Result.TRUE) || hasPermission(who, "role", "admin",
                region).equals(Result.TRUE);
    }

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

    public static class CacheEntry {
        private final Object flag;
        private final String value;
        private final Object key;

        public Result result;

        public CacheEntry(Object flag, String value, Object key, Result result) {
            this.flag = flag;
            this.value = value;
            this.key = key;
            this.result = result;
        }

        // Getters
        public Object getFlag() {
            return flag;
        }

        public String getValue() {
            return value;
        }

        public Object getKey() {
            return key;
        }

        public boolean isEqual(Object flag, String value, Object key) {
            return this.flag.equals(flag) && this.value.equals(value) && this.key.equals(key);
        }
    }
}
