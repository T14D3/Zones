package de.t14d3.zones;


import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;


public class Utils {


    private Zones plugin;

    public Utils(Zones plugin) {
        this.plugin = plugin;
    }

    public void createBeacon(Player player, Location location, DyeColor color) {
        if (location == null || player == null) {
            return;
        }

        Material glassMaterial = Material.getMaterial(color.name() + "_STAINED_GLASS");
        BlockData glassData = Bukkit.createBlockData(glassMaterial);

        int x = location.getBlockX();
        int y = -62;
        int z = location.getBlockZ();
        World world = location.getWorld();

        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
            for (int xPoint = x - 1; xPoint <= x + 1; xPoint++) {
                for (int zPoint = z - 1; zPoint <= z + 1; zPoint++) {
                    player.sendBlockChange(world.getBlockAt(xPoint, y, zPoint).getLocation(), Material.IRON_BLOCK.createBlockData());
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
        }, 10L);
    }
    public static void resetBeacon(Player player, Location location) {
        if (location == null || player == null) {
            return;
        }

        int x = location.getBlockX();
        int y = 0;
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
    public boolean isContainer(BlockState state) {
        return state instanceof Container;
    }
    public boolean isPowerable(BlockData data) {
        return data instanceof Powerable;
    }
}
