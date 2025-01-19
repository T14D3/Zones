package de.t14d3.zones;

import de.t14d3.zones.utils.Actions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PermissionManager {

    private RegionManager regionManager;

    private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<CacheEntry>> interactionCache = new ConcurrentHashMap<UUID, ConcurrentLinkedQueue<CacheEntry>>();
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> permissionCache = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>>();

    public PermissionManager() {}

    // Setter for RegionManager to avoid circular dependency
    public void setRegionManager(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    /**
     * Checks if a player can interact with a region.
     * @param location The location of the interaction.
     * @param playerUUID The UUID of the player.
     * @param action The action the player wants to perform.
     * @param type The type of the block or entity the interaction happened with.
     * @return True if the player can interact with the region, false otherwise.
     */
    public boolean canInteract(Location location, UUID playerUUID, Actions action, String type) {

        // Skip checking if player has global bypass permission
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.hasPermission("zones.bypass.claimed")) {
            return true;
        }
        if (interactionCache.containsKey(playerUUID)) {
            ConcurrentLinkedQueue<CacheEntry> entries = interactionCache.get(playerUUID);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(location, action.name(), type)) {
                    return entry.result.equals(Result.TRUE);
                }
            }
        }
        if (!regionManager.getRegionsAt(location).isEmpty()) {
            Result result = Result.UNDEFINED;
            int priority = Integer.MIN_VALUE;

            for (Region region : regionManager.getRegionsAt(location)) {

                // Only check regions with a higher priority than the current value
                if (region.getPriority() > priority) {
                    Result hasPermission = hasPermission(playerUUID.toString(), action.name(), type, region);
                    if (!hasPermission.equals(Result.UNDEFINED)) {
                        result = hasPermission;
                        priority = region.getPriority();
                        continue;
                    }
                }
                // If same priority, both have to be true, otherwise will assume false
                else if (region.getPriority() == priority) {
                    Result hasPermission = hasPermission(playerUUID.toString(), action.name(), type, region);
                    if (hasPermission.equals(Result.FALSE) || result.equals(Result.FALSE)) {
                        result = hasPermission;
                        priority = region.getPriority();
                        continue;
                    }
                }
            }

            return result.equals(Result.TRUE);
        }
        // If no region found, check for unclaimed bypass perm and return false if not found
        return player != null && player.hasPermission("zones.bypass.unclaimed");
    }

    /**
     * Invalidates the interaction cache for a player.
     * Should be called e.g. when a player logs out.
     *
     * @param target The player to invalidate the cache for.
     */
    public void invalidateInteractionCache(UUID target) {
        interactionCache.remove(target);
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
     * @return true if the player has the specified permission for the type, false otherwise.
     * @see #hasPermission(String, String, String, Region)
     */
    public boolean hasPermission(UUID uuid, String permission, String type, Region region) {
        return hasPermission(uuid.toString(), permission, type, region).equals(Result.TRUE);
    }
    /**
     * Checks if a player has a specific permission for a given type in the provided region.
     *
     * @param who    Who to check the permission for
     * @param permission The permission being checked (e.g., "break", "place").
     * @param type    The type of object the permission applies to (e.g., "GRASS_BLOCK").
     * @param region  The region in which the permission is being checked.
     * @return {@link Result}
     */
    public Result hasPermission(String who, String permission, String type, Region region) {
        if (permissionCache.containsKey(who)) {
            ConcurrentLinkedQueue<CacheEntry> entries = permissionCache.get(who);
            for (CacheEntry entry : entries) {
                if (entry.isEqual(permission, type, region.getKey())) {
                    return entry.result;
                }
            }
        }
        Result result = Result.UNDEFINED; // Initialize result as null
        result = calculatePermission(who, permission, type, region);
        permissionCache.computeIfAbsent(who, k -> new ConcurrentLinkedQueue<>()).add(new CacheEntry(permission, type, region.getKey(), result));
        return result;
    }

    private Result calculatePermission(String who, String permission, String type, Region region) {
        permission = permission.toLowerCase();
        Player player = null;
        try {
            player = Bukkit.getPlayer(UUID.fromString(who));
        } catch (IllegalArgumentException ignored) {
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
                return hasPermission(who, permission, type, region.getParentRegion(this.regionManager));
            }
            if (permissions.containsKey("group")) {
                if (who.startsWith("+group-") && !Zones.getInstance().getConfig().getBoolean("allow-group-recursion", false)) {
                    Zones.getInstance().getLogger().severe("Recursive group permissions detected!! Groups are not allowed to contain other groups!");
                    Zones.getInstance().getLogger().severe("Group '" + who.substring(7) + "' contains 'group' permission entry in region '" + region.getKey() + "'");
                    Zones.getInstance().getLogger().severe("If you are 100% sure this is fine, add 'allow-group-recursion: true' to your config.yml");
                    return Result.FALSE;
                }
                for (String group : permissions.get("group").split(",")) {
                    if (hasPermission(group, permission, type, region).equals(Result.TRUE)) {
                        result = Result.TRUE;
                    }
                }
            }
            // Nothing found
            else {
                return result; // Return initial result
            }
        }

        if (value != null) {
            for (String permittedValue : value.split(",")) {
                permittedValue = permittedValue.trim(); // Trim whitespace

                // Check for wildcard allow
                if ("*".equals(permittedValue) || "true".equalsIgnoreCase(permittedValue)) {
                    result = Result.TRUE;
                }
                // Check for wildcard deny
                else if ("!*".equals(permittedValue) || "false".equalsIgnoreCase(permittedValue)) {
                    result = Result.FALSE;
                }
                // Check for specific type allow
                else if (permittedValue.equalsIgnoreCase(type)) {
                    result = Result.TRUE;
                }
                // Check for specific type deny
                else if (permittedValue.equalsIgnoreCase("!" + type)) {
                    result = Result.FALSE;
                }
            }
        }

        return result; // Return null if no permission was set
    }

    public boolean isAdmin(String who, Region region) {
        return hasPermission(who, "role", "owner", region).equals(Result.TRUE)
                || hasPermission(who, "role", "admin", region).equals(Result.TRUE);
    }

    public static class CacheEntry {
        private final Object flag;
        private final String value;
        private final String key;

        public Result result;

        public CacheEntry(Object flag, String value, String key, Result result) {
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

        public String getKey() {
            return key;
        }

        public boolean isEqual(Object flag, String value, String key) {
            return this.flag.equals(flag) && this.value.equals(value) && this.key.equals(key);
        }
    }

    /**
     * The result of a permission check
     * TRUE/FALSE overwrite UNDEFINED
     */
    public enum Result {
        TRUE,
        FALSE,
        UNDEFINED
    }
}
