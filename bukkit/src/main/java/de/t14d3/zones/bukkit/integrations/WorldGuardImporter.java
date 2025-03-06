package de.t14d3.zones.integrations;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionType;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.RegionFlagEntry;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldGuardImporter {

    private final ZonesBukkit plugin;

    public WorldGuardImporter(ZonesBukkit plugin) {
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
                    RegionKey key = RegionKey.generate();
                    String name = entry.getKey();
                    BlockLocation min = BlockLocation.of(BukkitAdapter.adapt(world, region.getMinimumPoint()));
                    BlockLocation max = BlockLocation.of(BukkitAdapter.adapt(world, region.getMaximumPoint()));

                    Map<String, List<RegionFlagEntry>> members = new HashMap<>();
                    region.getMembers().getUniqueIds().forEach(uuid -> {
                        members.put(uuid.toString(), List.of(new RegionFlagEntry("group", "member", false)));
                    });

                    region.getOwners().getUniqueIds().forEach(uuid -> {
                        members.put(uuid.toString(), List.of(new RegionFlagEntry("role", "owner", false)));
                    });

                    List<RegionFlagEntry> memberPermissions = new ArrayList<>();
                    memberPermissions.add(new RegionFlagEntry("break", "true", false));
                    memberPermissions.add(new RegionFlagEntry("place", "true", false));
                    memberPermissions.add(new RegionFlagEntry("container", "true", false));
                    memberPermissions.add(new RegionFlagEntry("redstone", "true", false));
                    memberPermissions.add(new RegionFlagEntry("interact", "true", false));
                    memberPermissions.add(new RegionFlagEntry("entity", "true", false));
                    memberPermissions.add(new RegionFlagEntry("damage", "true", false));
                    members.put("+group-members", memberPermissions);


                    Region newRegion = plugin.getRegionManager()
                            .createNewRegion(key, name, min, max, de.t14d3.zones.objects.World.of(world), members,
                                    region.getPriority());
                    count[0]++;
                }

            }

            plugin.getLogger().info("Imported " + count[0] + " regions from WorldGuard.");
            plugin.getRegionManager().triggerSave();
        }).start();
    }
}
