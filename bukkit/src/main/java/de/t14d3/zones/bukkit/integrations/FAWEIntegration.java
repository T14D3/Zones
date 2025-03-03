package de.t14d3.zones.integrations;

import com.fastasyncworldedit.bukkit.regions.BukkitMaskManager;
import com.fastasyncworldedit.core.regions.FaweMask;
import com.fastasyncworldedit.core.util.WEManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.t14d3.zones.Region;
import de.t14d3.zones.bukkit.BukkitPlatform;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Result;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.flags.Flags;
import org.bukkit.Location;
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
        return region != null && (region.isAdmin(
                player.getUniqueId()) || type == MaskType.MEMBER && (Flags.BREAK.getCustomHandler()
                .evaluate(region, player.getUniqueId().toString(), "break", "true")
                .equals(Result.TRUE) || Flags.PLACE.getCustomHandler()
                .evaluate(region, player.getUniqueId().toString(), "place", "true").equals(Result.TRUE)));
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, final MaskType type, boolean isWhitelist) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        Region region = plugin.getRegionManager()
                .getEffectiveRegionAt(BlockLocation.of(location), World.of(location.getWorld()));
        if (region == null) {
            return null;
        }
        if (isAllowed(player, region, type)) {
            final Location pos1 = region.getMin().toLocation(BukkitPlatform.getWorld(region.getWorld()));
            final Location pos2 = region.getMax().toLocation(BukkitPlatform.getWorld(region.getWorld()));
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
}
