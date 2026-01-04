package de.t14d3.zones.permissions;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.objects.Result;
import de.t14d3.zones.permissions.flags.FlagContext;
import de.t14d3.zones.permissions.subjects.GroupSubject;
import de.t14d3.zones.permissions.subjects.PlayerSubject;
import de.t14d3.zones.permissions.subjects.SubjectRef;
import de.t14d3.zones.permissions.subjects.Subjects;
import de.t14d3.zones.rapunzellib.ZonesPermissionCache;
import de.t14d3.zones.utils.DebugLoggerManager;
import de.t14d3.zones.utils.TypeKeys;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PermissionManager {
    private final DebugLoggerManager debugLogger;
    private final Zones zones;
    private final List<Permission> permissionMap;

    private final AtomicLong cacheVersion = new AtomicLong(1);
    private final RegionPermissionEvaluator evaluator;
    private final long permissionCacheTtlNanos;

    public PermissionManager(Zones zones) {
        this.zones = zones;
        this.debugLogger = zones.getDebugLogger();

        // Load the permission registry shipped with the plugin (permissions.json).
        // This is exposed for help/inspection; permission evaluation itself is based on region data.
        List<Permission> permissions = new ArrayList<>();
        Gson gson = new Gson();
        JsonObject obj = gson.fromJson(
                new JsonReader(new InputStreamReader(zones.getClass().getResourceAsStream("/permissions.json"))),
                JsonObject.class);
        for (Map.Entry<String, JsonElement> entry : obj.getAsJsonObject("permissions").entrySet()) {
            String key = entry.getKey();
            JsonObject permission = entry.getValue().getAsJsonObject();
            String description = permission.get("description").getAsString();
            int level = permission.get("level").getAsInt();
            permissions.add(new Permission(key, description, level));
        }
        this.permissionMap = permissions;

        long ttlSeconds = zones.getConfig().getInt("cache.ttl", 300);
        int limit = zones.getConfig().getInt("cache.limit", 0);
        int permissionTtlSeconds = zones.getConfig().getInt("cache.permission-ttl", 2);
        this.permissionCacheTtlNanos = TimeUnit.SECONDS.toNanos(Math.max(0, permissionTtlSeconds));

        this.evaluator = new RegionPermissionEvaluator(
                zones.getRegionManager(),
                cacheVersion::get,
                ttlSeconds * 1000L,
                limit
        );
    }

    /**
     * Invalidates all cached permission decisions.
     */
    public void invalidateAll() {
        cacheVersion.incrementAndGet();
        evaluator.invalidateAll();
    }

    /**
     * Invalidates cached decisions for a specific subject (player UUID).
     */
    public void invalidateSubject(UUID uuid) {
        if (uuid == null) return;
        evaluator.invalidateSubject(new PlayerSubject(uuid));
    }


    /**
     * Checks whether a player is allowed to perform an action at a given location.
     *
     * @param location   block position
     * @param world      world reference
     * @param playerUUID player UUID
     * @param action     action flag
     * @param type       type key involved in the interaction
     */
    public boolean checkAction(RBlockPos location, RWorldRef world, UUID playerUUID, Flag action, String type) {
        if (playerUUID == null) return false;
        return checkAction(location, world, new PlayerSubject(playerUUID), action, type, FlagContext.none());
    }


    /**
     * Checks if a player can interact with a region.
     *
     * @param location The location of the interaction.
     * @param who      The UUID of the player.
     * @param action   The action the player wants to perform.
     * @param type     The type of the block or entity the interaction happened with.
     * @return True if the player can interact with the region, false otherwise.
     */
    public boolean checkAction(RBlockPos location, RWorldRef world, String who, Flag action, String type) {
        return checkAction(location, world, parseSubject(who), action, type, FlagContext.none());
    }

    /**
     * Checks if a player can interact with a region.
     *
     * @param location The location of the interaction.
     * @param subject  The subject of the interaction.
     * @param action   The action the player wants to perform.
     * @param type     The type of the block or entity the interaction happened with.
     * @param context  Additional, optional information, for example a spawn reason.
     * @return True if the player can interact with the region, false otherwise.
     */
    public boolean checkAction(RBlockPos location, RWorldRef world, SubjectRef subject, Flag action, String type, FlagContext context) {
        if (action == null) return true;
        FlagContext ctx = context != null ? context : FlagContext.none();
        if (location == null || world == null) return action.getDefaultValue(ctx);

        final SubjectRef effectiveSubject = subject != null ? subject : Subjects.universal();
        final String subjectLabel = Subjects.format(effectiveSubject);

        String normalizedType = TypeKeys.normalize(type);
        debugLogger.log(DebugLoggerManager.CHECK, action.name(), subjectLabel, location, normalizedType);

        return zones.getRegionManager().withWorldReadLock(world, () -> {
            List<Region> regions = zones.getRegionManager().getRegionsAt(location, world);
            if (regions.isEmpty()) {
                if (effectiveSubject instanceof PlayerSubject p) {
                    boolean bypass = RPlayer.get(p.uuid())
                            .map(player -> ZonesPermissionCache.hasPermissionCached(player, "zones.bypass.unclaimed",
                                    permissionCacheTtlNanos))
                            .orElse(false);
                    debugLogger.log(DebugLoggerManager.PERM, action.name(), subjectLabel, location, normalizedType,
                            bypass);
                    return bypass;
                }
                return action.getDefaultValue(ctx);
            }

            int maxPriority = Integer.MIN_VALUE;
            for (Region region : regions) {
                if (region.getPriority() > maxPriority) maxPriority = region.getPriority();
            }

            boolean anyAllow = false;
            for (Region region : regions) {
                if (region.getPriority() != maxPriority) continue;

                Decision decision = evaluator.evaluate(region, effectiveSubject, action, normalizedType);
                if (decision.result() == Result.FALSE) return false;
                if (decision.result() == Result.TRUE) anyAllow = true;
            }

            if (anyAllow) return true;
            return action.getDefaultValue(ctx);
        });
    }

    /**
     * Checks if a universal/non-player action is allowed at a location.
     * This method bypasses player-specific checks for efficiency.
     *
     * @param location The location of the interaction
     * @param action   The action being performed
     * @param type     The type of block/entity involved
     * @return true if the action is allowed, false otherwise
     */
    public boolean checkAction(RBlockPos location, RWorldRef world, Flag action, String type) {
        return checkAction(location, world, Subjects.universal(), action, type, FlagContext.none());
    }

    /**
     * Checks if a universal/non-player action is allowed at a location.
     * This method bypasses player-specific checks for efficiency.
     *
     * @param location The location of the interaction
     * @param action   The action being performed
     * @param type     The type of block/entity involved
     * @param context  Additional context (used for flag defaults)
     * @return true if the action is allowed, false otherwise
     */
    public boolean checkAction(RBlockPos location, RWorldRef world, Flag action, String type, FlagContext context) {
        return checkAction(location, world, Subjects.universal(), action, type, context);
    }

    /**
     * Checks whether a player is allowed to perform multiple actions at a location.
     *
     * <p>This batches evaluation under a single world read-lock and reuses the same region query for all flags.
     * Semantics match calling {@link #checkAction(RBlockPos, RWorldRef, UUID, Flag, String)} for each flag.</p>
     *
     * @param location   block position
     * @param world      world reference
     * @param playerUUID player UUID
     * @param type       type key involved in the interaction
     * @param actions    action flags to evaluate
     * @return {@code true} if all actions are allowed, otherwise {@code false}
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkActions(RBlockPos location, RWorldRef world, UUID playerUUID, String type, Flag... actions) {
        if (playerUUID == null) return false;
        if (actions == null || actions.length == 0) return true;
        if (location == null || world == null) {
            for (Flag action : actions) {
                if (action == null) continue;
                if (!action.getDefaultValue()) return false;
            }
            return true;
        }

        SubjectRef subject = new PlayerSubject(playerUUID);
        String normalizedType = TypeKeys.normalize(type);

        return zones.getRegionManager().withWorldReadLock(world, () -> {
            List<Region> regions = zones.getRegionManager().getRegionsAt(location, world);
            if (regions.isEmpty()) {
                boolean bypass = RPlayer.get(playerUUID)
                        .map(p -> ZonesPermissionCache.hasPermissionCached(p, "zones.bypass.unclaimed",
                                permissionCacheTtlNanos))
                        .orElse(false);

                for (Flag action : actions) {
                    if (action == null) continue;
                    debugLogger.log(DebugLoggerManager.CHECK, action.name(), Subjects.format(subject), location,
                            normalizedType);
                    debugLogger.log(DebugLoggerManager.PERM, action.name(), Subjects.format(subject), location,
                            normalizedType, bypass);
                }

                // In unclaimed land, bypass is a global allow/deny for players (matches single-flag behavior).
                return bypass;

            }

            int maxPriority = Integer.MIN_VALUE;
            for (Region region : regions) {
                int priority = region.getPriority();
                if (priority > maxPriority) maxPriority = priority;
            }

            for (Flag action : actions) {
                if (action == null) continue;

                debugLogger.log(DebugLoggerManager.CHECK, action.name(), Subjects.format(subject), location,
                        normalizedType);

                boolean anyAllow = false;
                for (Region region : regions) {
                    if (region.getPriority() != maxPriority) continue;
                    Decision decision = evaluator.evaluate(region, subject, action, normalizedType);
                    if (decision.result() == Result.FALSE) return false;
                    if (decision.result() == Result.TRUE) anyAllow = true;
                }

                if (anyAllow) continue;
                if (!action.getDefaultValue()) return false;
            }

            return true;
        });
    }

    private static SubjectRef parseSubject(String raw) {
        if (raw == null || raw.isBlank()) return Subjects.universal();
        String s = raw.trim();
        if ("universal".equalsIgnoreCase(s)
                || "+universal".equalsIgnoreCase(s)) {
            return Subjects.universal();
        }
        if (s.regionMatches(true, 0, "group:", 0, "group:".length())) {
            String name = Subjects.normalizeGroupName(s.substring("group:".length()));
            return name == null ? Subjects.universal() : new GroupSubject(name);
        }
        if (s.regionMatches(true, 0, "player:", 0, "player:".length())) s = s.substring("player:".length());
        try {
            return new PlayerSubject(UUID.fromString(s));
        } catch (IllegalArgumentException ignored) {
            return Subjects.universal();
        }
    }

    /**
     * Gets a list of all permissions recognized this platform.
     *
     * @return List of {@link Permission} objects.
     */
    public List<Permission> getPermissions() {
        return permissionMap;
    }

    /**
     * Simple permission object.
     * Contains a name, description, and level (Vanilla operator level equivalent).
     */
    public record Permission(String name, String description, int level) {
        /**
         * Creates a new permission object.
         *
         * @param name       The permission name.
         * @param description The permission description.
         * @param level       The permission level.
         */
        public Permission {}
        }

}
