package de.t14d3.zones.bukkit.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionType;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.permissions.subjects.Subjects;
import org.bukkit.World;

/**
 * Imports WorldGuard regions into Zones.
 *
 * <p>Currently supports cuboid regions only. Import runs on a background thread and creates new Zones regions with
 * generated keys.</p>
 */
public class WorldGuardImporter {

    private final ZonesBukkit plugin;
    private static final String MEMBER_GROUP = "member";

    /**
     * Creates a new importer instance.
     *
     * @param plugin owning plugin instance
     */
    public WorldGuardImporter(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts importing regions from WorldGuard.
     *
     * <p>This method returns immediately; the import runs on a background thread and triggers a save after completion.</p>
     */
    public void importRegions() {
        final int[] count = {0};
        new Thread(() -> {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

            for (World world : plugin.getServer().getWorlds()) {
                var manager = container.get(BukkitAdapter.adapt(world));
                if (manager == null) continue;

                for (var entry : manager.getRegions().entrySet()) {
                    ProtectedRegion region = entry.getValue();
                    if (!region.getType().equals(RegionType.CUBOID)) {
                        continue;
                    }
                    RegionKey key = RegionKey.generate();
                    String name = entry.getKey();
                    var minLoc = BukkitAdapter.adapt(world, region.getMinimumPoint());
                    var maxLoc = BukkitAdapter.adapt(world, region.getMaximumPoint());
                    RBlockPos min = new RBlockPos(minLoc.getBlockX(), minLoc.getBlockY(), minLoc.getBlockZ());
                    RBlockPos max = new RBlockPos(maxLoc.getBlockX(), maxLoc.getBlockY(), maxLoc.getBlockZ());
                    RWorldRef worldRef = new RWorldRef(world.getName(), world.getKey().toString());

                    Region newRegion = plugin.getRegionManager()
                            .createNewRegion(key, name, min, max, worldRef, new RegionPermissions(),
                                    region.getPriority());

                    plugin.getRegionManager().withWorldWriteLock(newRegion.getWorld(), () -> {
                        RegionPermissions perms = newRegion.getPermissions();
                        RegionMembership membership = newRegion.getMembership();

                        for (var uuid : region.getMembers().getUniqueIds()) {
                            membership.player(uuid).groups().allow(MEMBER_GROUP);
                        }

                        for (var uuid : region.getOwners().getUniqueIds()) {
                            membership.player(uuid).roles().allow("owner");
                        }

                        var membersSubject = Subjects.group(MEMBER_GROUP);
                        Flag[] defaultMemberAllowed = {
                                Flags.BREAK,
                                Flags.PLACE,
                                Flags.CONTAINER,
                                Flags.REDSTONE,
                                Flags.INTERACT,
                                Flags.ENTITY,
                                Flags.DAMAGE
                        };

                        allowAll(perms, membersSubject, defaultMemberAllowed);
                        importWorldGuardFlags(region, perms);

                        plugin.getRegionManager().saveRegion(key, newRegion);
                    });
                    plugin.getZones().getPermissionManager().invalidateAll();
                    count[0]++;
                }

            }

            plugin.getLogger().info("Imported " + count[0] + " regions from WorldGuard.");
            plugin.getRegionManager().triggerSave();
        }).start();
    }

    private static void allowAll(RegionPermissions perms, de.t14d3.zones.permissions.subjects.SubjectRef subject, Flag[] flags) {
        if (perms == null || subject == null || flags == null) return;
        for (Flag flag : flags) {
            if (flag == null) continue;
            RegionPermissions.TargetDecisionValue v = (RegionPermissions.TargetDecisionValue) perms
                    .subject(subject)
                    .getOrCreate(flag.id(), FlagValueKind.TARGET_DECISION);
            v.allow("*");
        }
    }

    private static void importWorldGuardFlags(ProtectedRegion wgRegion, RegionPermissions perms) {
        if (wgRegion == null || perms == null) return;

        // Prefer the specific action flags (block-break/place) and fall back to build.
        importStateOrFallback(wgRegion, perms,
                com.sk89q.worldguard.protection.flags.Flags.BLOCK_BREAK,
                com.sk89q.worldguard.protection.flags.Flags.BUILD,
                Flags.BREAK);
        importStateOrFallback(wgRegion, perms,
                com.sk89q.worldguard.protection.flags.Flags.BLOCK_PLACE,
                com.sk89q.worldguard.protection.flags.Flags.BUILD,
                Flags.PLACE);

        // Interactions and "use" are separate in WorldGuard; either can express the intended behavior.
        importStateOrFallback(wgRegion, perms,
                com.sk89q.worldguard.protection.flags.Flags.INTERACT,
                com.sk89q.worldguard.protection.flags.Flags.USE,
                Flags.INTERACT, Flags.REDSTONE);

        // Container access maps reasonably well to Zones' container flag.
        importStateOrFallback(wgRegion, perms,
                com.sk89q.worldguard.protection.flags.Flags.CHEST_ACCESS,
                com.sk89q.worldguard.protection.flags.Flags.USE,
                Flags.CONTAINER);

        // Note: WorldGuard doesn't expose a single 1:1 flag for all redstone interactions; we approximate this by
        // mapping INTERACT/USE into Zones' redstone flag above.
    }

    private static void importStateOrFallback(
            ProtectedRegion wgRegion,
            RegionPermissions perms,
            StateFlag primary,
            StateFlag fallback,
            Flag... zonesFlags
    ) {
        if (wgRegion == null || perms == null) return;
        if (primary != null) {
            StateFlag.State state = wgRegion.getFlag(primary);
            if (state != null) {
                applyState(perms, state, resolveGroup(wgRegion, primary), zonesFlags);
                return;
            }
        }
        if (fallback != null) {
            importStateFlag(wgRegion, perms, fallback, zonesFlags);
        }
    }

    private static void importStateFlag(ProtectedRegion wgRegion, RegionPermissions perms, StateFlag flag, Flag... zonesFlags) {
        if (wgRegion == null || perms == null || flag == null) return;
        StateFlag.State state = wgRegion.getFlag(flag);
        if (state == null) return;
        applyState(perms, state, resolveGroup(wgRegion, flag), zonesFlags);
    }

    private static RegionGroup resolveGroup(ProtectedRegion wgRegion, StateFlag flag) {
        if (wgRegion == null || flag == null) return RegionGroup.ALL;
        RegionGroup group = wgRegion.getFlag(flag.getRegionGroupFlag());
        return group != null ? group : RegionGroup.ALL;
    }

    private static void applyState(RegionPermissions perms, StateFlag.State state, RegionGroup group, Flag... zonesFlags) {
        if (perms == null || state == null || zonesFlags == null) return;
        if (group == null) group = RegionGroup.ALL;

        // WorldGuard can target different membership groups per flag. Zones can't express "nonmembers" directly,
        // so we approximate by applying to universal and (for DENY) explicitly allowing members.
        for (Flag zoneFlag : zonesFlags) {
            if (zoneFlag == null) continue;

            switch (group) {
                case MEMBERS -> applyStateToSubject(perms, Subjects.group(MEMBER_GROUP), zoneFlag, state);
                case NON_MEMBERS -> {
                    applyStateToSubject(perms, Subjects.universal(), zoneFlag, state);
                    if (state == StateFlag.State.DENY) {
                        applyStateToSubject(perms, Subjects.group(MEMBER_GROUP), zoneFlag, StateFlag.State.ALLOW);
                    }
                }
                case OWNERS -> {
                    // Owners are treated as admins in Zones (bypass), so importing owner-only flags is skipped.
                }
                case ALL -> {
                    applyStateToSubject(perms, Subjects.universal(), zoneFlag, state);
                    // The importer creates explicit member allows as a baseline (WorldGuard-like default behavior),
                    // so ALL-group flags must also apply to members to override those defaults.
                    applyStateToSubject(perms, Subjects.group(MEMBER_GROUP), zoneFlag, state);
                }
            }
        }
    }

    private static void applyStateToSubject(
            RegionPermissions perms,
            de.t14d3.zones.permissions.subjects.SubjectRef subject,
            Flag zoneFlag,
            StateFlag.State state
    ) {
        RegionPermissions.TargetDecisionValue v = (RegionPermissions.TargetDecisionValue) perms
                .subject(subject)
                .getOrCreate(zoneFlag.id(), FlagValueKind.TARGET_DECISION);
        if (state == StateFlag.State.ALLOW) v.allow("*");
        if (state == StateFlag.State.DENY) v.deny("*");
    }
}
