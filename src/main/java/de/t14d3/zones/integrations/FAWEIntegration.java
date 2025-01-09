package de.t14d3.zones.integrations;

import com.fastasyncworldedit.bukkit.regions.BukkitMaskManager;
import com.fastasyncworldedit.core.regions.FaweMask;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public class FAWEIntegration extends BukkitMaskManager {

    private final Zones plugin;

    public FAWEIntegration(final Zones plugin) {
        super(plugin.getName());
        this.plugin = plugin;
    }

    public boolean isAllowed(Player player, Region region, MaskType type) {
        return region != null &&
                (region.isAdmin(player.getUniqueId()) ||
                        type == MaskType.MEMBER && (
                                plugin.getPermissionManager().hasPermission(player.getUniqueId(), "PLACE", "true", region)
                                        || plugin.getPermissionManager().hasPermission(player.getUniqueId(), "BREAK", "true", region)));
    }

    @Override
    public FaweMask getMask(final com.sk89q.worldedit.entity.Player wePlayer, final MaskType type, boolean isWhitelist) {
        final Player player = BukkitAdapter.adapt(wePlayer);
        final Location location = player.getLocation();
        List<Region> regions = plugin.getRegionManager().getRegionsAt(location);
        if (!regions.isEmpty()) {
            boolean isAllowed = true;
            for (Region region : regions) {
                if (!isAllowed(player, region, type)) {
                    isAllowed = false;
                    break;
                }
            }
            if (isAllowed) {
                final Location pos1 = regions.get(0).getMin();
                final Location pos2 = regions.get(0).getMax();
                final Region region = regions.get(0);
                return new FaweMask(new CuboidRegion(BukkitAdapter.asBlockVector(pos1), BukkitAdapter.asBlockVector(pos2))) {
                    @Override
                    public boolean isValid(com.sk89q.worldedit.entity.Player player, MaskType type) {
                        return isAllowed(BukkitAdapter.adapt(player), region, type);
                    }
                };
            }
        }


        return null;
    }

}
