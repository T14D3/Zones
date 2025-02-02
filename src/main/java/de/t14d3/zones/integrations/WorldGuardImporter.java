package de.t14d3.zones.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionType;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldGuardImporter {

    private final Zones plugin;

    public WorldGuardImporter(Zones plugin) {
        this.plugin = plugin;
    }

    public void importRegions() {
        final int[] count = {0};
        new Thread(() -> {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

            for (World world : plugin.getServer().getWorlds()) {
                for (Map.Entry<String, ProtectedRegion> entry : container.get(BukkitAdapter.adapt(world)).getRegions()
                        .entrySet()) {
                    ProtectedRegion region = entry.getValue();
                    if (!region.getType().equals(RegionType.CUBOID)) {
                        continue;
                    }
                    String key;
                    do {
                        key = UUID.randomUUID().toString().substring(0, 8);
                    } while (plugin.getRegionManager().regions().containsKey(key));
                    String name = entry.getKey();
                    Location min = BukkitAdapter.adapt(world, region.getMinimumPoint());
                    Location max = BukkitAdapter.adapt(world, region.getMaximumPoint());

                    Map<String, Map<String, String>> members = new HashMap<>();
                    members.put("+group-members",
                            Map.of("break", "true", "place", "true", "container", "true", "redstone", "true",
                                    "interact", "true", "entity", "true", "damage", "true"));

                    region.getMembers().getUniqueIds()
                            .forEach(uuid -> members.put(uuid.toString(), Map.of("group", "member")));
                    region.getOwners().getUniqueIds()
                            .forEach(uuid -> members.put(uuid.toString(), Map.of("role", "owner")));

                    Region newRegion = plugin.getRegionManager()
                            .createNewRegion(key, name, min, max, members, region.getPriority());
                    plugin.getRegionManager().addRegion(newRegion);
                    count[0]++;
                }

            }

            plugin.getLogger().info("Imported " + count[0] + " regions from WorldGuard.");
            plugin.getRegionManager().triggerSave();
        }).start();
    }
}
