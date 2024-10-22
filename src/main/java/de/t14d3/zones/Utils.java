package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class Utils {

    public static void createBeacon(Player player, Location location, DyeColor color) {
        Material glassMaterial = Material.getMaterial(color.name() + "_STAINED_GLASS");
        BlockData glassData = Bukkit.createBlockData(glassMaterial);
        player.sendBlockChange(location, glassData);

        Location beaconLocation = location.clone().add(0, -1, 0);
        BlockData beaconData = Bukkit.createBlockData(Material.BEACON);
        player.sendBlockChange(beaconLocation, beaconData);

        // Create a 3x3 platform of iron blocks below the beacon
        Location platformStart = beaconLocation.clone().add(-1, -1, -1);
        BlockData ironBlockData = Bukkit.createBlockData(Material.IRON_BLOCK);

        // Loop to create the 3x3 platform
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                Location ironBlockLocation = platformStart.clone().add(x, 0, z);
                player.sendBlockChange(ironBlockLocation, ironBlockData);
            }
        }
    }

}
