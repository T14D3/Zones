package de.t14d3.zones.integrations;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.Result;
import de.t14d3.zones.permissions.flags.Flags;
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
            event.setExtent(new WorldEditExtent(mask, event.getExtent()));
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
            for (Region region : plugin.getRegionManager().regions().values()) {
                if ((Flags.BREAK.getCustomHandler().evaluate(region, player.getUniqueId().toString(), "break", "true")
                        .equals(Result.TRUE) || Flags.PLACE.getCustomHandler()
                        .evaluate(region, player.getUniqueId().toString(), "place", "true")
                        .equals(Result.TRUE)) || region.isAdmin(player.getUniqueId())) {
                    mask.add(new CuboidRegion(
                            BlockVector3.at(region.getMin().getX(), region.getMin().getY(), region.getMin().getZ()),
                            BlockVector3.at(region.getMax().getX() - 1, region.getMax().getY() - 1,
                                    region.getMax().getZ() - 1)
                            // Don't ask me why, but it works
                    ));
                }
            }
            return mask;
        }
    }

}
