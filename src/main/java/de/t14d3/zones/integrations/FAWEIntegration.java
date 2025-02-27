package de.t14d3.zones.integrations;

import com.fastasyncworldedit.bukkit.regions.BukkitMaskManager;
import com.fastasyncworldedit.core.regions.FaweMask;
import com.fastasyncworldedit.core.util.WEManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.Result;
import de.t14d3.zones.permissions.flags.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class FAWEIntegration extends BukkitMaskManager {

    private final Zones plugin;

    public FAWEIntegration(final Zones plugin) {
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
        Region region = plugin.getRegionManager().getEffectiveRegionAt(location);
        if (region == null) {
            return null;
        }
        if (isAllowed(player, region, type)) {
            final Location pos1 = new Location(region.getWorld(), region.getMin().getX(), region.getMin().getY(),
                    region.getMin().getZ());
            final Location pos2 = new Location(region.getWorld(), region.getMax().getX(), region.getMax().getY(),
                    region.getMax().getZ());
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
