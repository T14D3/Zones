package de.t14d3.zones.bukkit.integrations;

import com.fastasyncworldedit.bukkit.regions.BukkitMaskManager;
import com.fastasyncworldedit.core.regions.FaweMask;
import com.fastasyncworldedit.core.util.WEManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.permissions.flags.Flags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

public class FAWEIntegration extends BukkitMaskManager {

    private final ZonesBukkit plugin;

    public FAWEIntegration(final ZonesBukkit plugin) {
        super(plugin.getName());
        this.plugin = plugin;
    }

    public void register() {
        WEManager.weManager().addManager(this);
    }

    public boolean isAllowed(Player player, Region region, MaskType type) {
        if (player == null || region == null) return false;
        if (player.hasPermission("zones.bypass.claimed")) return true;
        if (plugin.getRegionManager().withWorldReadLock(region.getWorld(), () -> region.isAdmin(player.getUniqueId())))
            return true;
        if (type != MaskType.MEMBER) return false;

        Location location = player.getLocation();
        return plugin.getPermissionManager().checkAction(location, player.getUniqueId(), Flags.BREAK, "*")
                || plugin.getPermissionManager().checkAction(location, player.getUniqueId(), Flags.PLACE, "*");
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, final MaskType type, boolean isWhitelist) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        RBlockPos pos = new RBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        RWorldRef world = new RWorldRef(location.getWorld().getName(), location.getWorld().getKey().toString());
        Region region = plugin.getRegionManager().getEffectiveRegionAt(pos, world);
        if (region == null) {
            return null;
        }
        if (isAllowed(player, region, type)) {
            org.bukkit.World bukkitWorld = resolveWorld(region.getWorld(), player);
            RBlockPos min = region.getMin();
            RBlockPos max = region.getMax();
            final Location pos1 = new Location(bukkitWorld, min.x(), min.y(), min.z());
            final Location pos2 = new Location(bukkitWorld, max.x(), max.y(), max.z());
            return new FaweMask(
                    new CuboidRegion(BukkitAdapter.asBlockVector(pos1), BukkitAdapter.asBlockVector(pos2))) {
                @Override
                public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                    return isAllowed(BukkitAdapter.adapt(player), region, type);
                }
            };
        }
        return null;
    }

    private static org.bukkit.World resolveWorld(RWorldRef world, Player fallbackPlayer) {
        if (world == null) return fallbackPlayer.getWorld();
        if (world.key() != null) {
            try {
                NamespacedKey key = NamespacedKey.fromString(world.key());
                if (key != null) {
                    org.bukkit.World byKey = Bukkit.getWorld(key);
                    if (byKey != null) return byKey;
                }
            } catch (Exception ignored) {
            }
        }
        if (world.name() != null) {
            org.bukkit.World byName = Bukkit.getWorld(world.name());
            if (byName != null) return byName;
        }
        org.bukkit.World byIdentifier = Bukkit.getWorld(world.identifier());
        if (byIdentifier != null) return byIdentifier;
        return fallbackPlayer.getWorld();
    }
}
