package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;

public class Utils {
    private Zones plugin;

    public Utils(Zones plugin) {
        this.plugin = plugin;
    }

    public static boolean isContainer(BlockState state) {
        return state instanceof Container;
    }

    public static boolean isPowerable(BlockData data) {
        return data instanceof Powerable;
    }
}
