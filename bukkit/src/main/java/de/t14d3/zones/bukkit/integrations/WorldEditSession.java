package de.t14d3.zones.bukkit.integrations;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class WorldEditSession {
    private final Zones plugin;
    private final WorldEditUtils utils;


    public WorldEditSession(Zones plugin) {
        this.plugin = plugin;
        this.utils = new WorldEditUtils();
    }

    @Subscribe
    public void onEditSessionEvent(EditSessionEvent event) {
        if (event.getStage() == EditSession.Stage.BEFORE_REORDER) {
            Player player = null;
            if (event.getActor().isPlayer()) {
                player = Bukkit.getPlayer(event.getActor().getName());
            }
            Set<CuboidRegion> mask = utils.getMask(player);
            event.setExtent(new WorldEditExtent(mask, event.getExtent(), plugin, player));      
        }
    }

    public class WorldEditUtils {
        public static boolean cuboidRegionContains(CuboidRegion region, int x, int y, int z) {
            return region.getMinimumPoint().x() <= x && region.getMaximumPoint().x() >= x && region.getMinimumPoint()
                    .y() <= y && region.getMaximumPoint().y() >= y && region.getMinimumPoint()
                    .z() <= z && region.getMaximumPoint().z() >= z;
        }

        public static boolean maskContains(Set<CuboidRegion> mask, int x, int y, int z) {
            for (CuboidRegion region : mask) {
                if (cuboidRegionContains(region, x, y, z)) {
                    return true;
                }
            }
            return false;
        }

        public HashSet<CuboidRegion> getMask(Player player) {
            HashSet<CuboidRegion> mask = new HashSet<>();
            if (player == null) return mask;

            String worldKey = player.getWorld().getKey().toString();
            for (Region region : plugin.getRegionManager().regions().values()) {
                if (region.getMin() == null || region.getMax() == null) continue;
                if (region.getWorld() != null && region.getWorld().key() != null && !region.getWorld().key()
                        .equals(worldKey)) {
                    continue;
                }

                mask.add(new CuboidRegion(
                        BlockVector3.at(region.getMin().x(), region.getMin().y(), region.getMin().z()),
                        BlockVector3.at(region.getMax().x() - 1, region.getMax().y() - 1,
                                region.getMax().z() - 1)
                        // Don't ask me why, but it works
                ));
            }
            return mask;
        }
    }

}
