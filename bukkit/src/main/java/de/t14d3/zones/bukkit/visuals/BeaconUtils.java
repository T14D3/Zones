package de.t14d3.zones.bukkit.visuals;

import de.t14d3.zones.bukkit.BukkitPlatform;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class BeaconUtils {

    private final ZonesBukkit plugin;

    public BeaconUtils(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    public static void resetBeacon(Player player, BlockLocation location, de.t14d3.zones.objects.World world) {
        resetBeacon(player, location.toLocation(BukkitPlatform.getWorld(world)));
    }

    /**
     * Resets the beacon at the given location.
     *
     * @param player   The player to remove the beacon from.
     * @param location The location where the beacon should be reset.
     */
    public static void resetBeacon(Player player, Location location) {
        if (location == null || player == null) {
            return;
        }

        int x = location.getBlockX();
        int y = -62;
        int z = location.getBlockZ();
        World world = location.getWorld();

        player.sendBlockChange(
                world.getBlockAt(x, 1, z).getLocation(),
                world.getBlockAt(x, 1, z).getBlockData()
        );

        for (int i = 2; i <= 255; i++) {
            if (world.getBlockAt(x, i, z).getType() != Material.AIR) {
                player.sendBlockChange(
                        world.getBlockAt(x, i, z).getLocation(),
                        world.getBlockAt(x, i, z).getBlockData()
                );
            }
        }

        for (int xPoint = x - 1; xPoint <= x + 1; xPoint++) {
            for (int zPoint = z - 1; zPoint <= z + 1; zPoint++) {
                player.sendBlockChange(
                        world.getBlockAt(xPoint, y, zPoint).getLocation(),
                        world.getBlockAt(x, y, z).getBlockData()
                );
            }
        }
    }

    /**
     * Creates a beacon at the given location with the given color.
     *
     * @param player   The player to display the beacon to.
     * @param location The location where the beacon should be created.
     * @param color    The color of the beacon.
     */
    public void createBeacon(Player player, Location location, DyeColor color) {
        if (location == null || player == null) {
            return;
        }

        Material glassMaterial = Material.getMaterial(color.name() + "_STAINED_GLASS");
        assert glassMaterial != null;
        BlockData glassData = Bukkit.createBlockData(glassMaterial);

        int x = location.getBlockX();
        int y = -62;
        int z = location.getBlockZ();
        World world = location.getWorld();

        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
            for (int xPoint = x - 1; xPoint <= x + 1; xPoint++) {
                for (int zPoint = z - 1; zPoint <= z + 1; zPoint++) {
                    player.sendBlockChange(world.getBlockAt(xPoint, y, zPoint).getLocation(),
                            Material.IRON_BLOCK.createBlockData());
                }
            }

            player.sendBlockChange(
                    world.getBlockAt(x, -61, z).getLocation(), Material.BEACON.createBlockData());

            int highestYPoint = 0;

            for (int yPoint = -60; yPoint <= 319; yPoint++) {
                if (world.getBlockAt(x, yPoint, z).getType() != Material.AIR) {
                    highestYPoint = yPoint;
                    player.sendBlockChange(world.getBlockAt(x, yPoint, z).getLocation(), glassData);
                }
            }

            player.sendBlockChange(world.getBlockAt(x, highestYPoint, z).getLocation(), glassData);
        }, 1L);
    }
}
