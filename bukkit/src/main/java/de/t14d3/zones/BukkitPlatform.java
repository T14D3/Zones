package de.t14d3.zones;

import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.PlayerRepository;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BukkitPlatform implements ZonesPlatform {
    private final ZonesBukkit plugin;

    public BukkitPlatform(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<World> getWorlds() {
        List<World> worlds = new ArrayList<>();
        plugin.getServer().getWorlds().forEach(world -> {
            worlds.add(World.of(world));
        });
        return worlds;
    }

    public static org.bukkit.World getWorld(World world) {
        return Bukkit.getServer().getWorld(world.getUID());
    }

    @Override
    public Player getPlayer(UUID uuid) {
        org.bukkit.entity.Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) {
            return PlayerRepository.getOrAdd(player.getName(), player.getUniqueId());
        }
        return null;
    }

    @Override
    public boolean hasPermission(Player player, String permission) {
        org.bukkit.entity.Player bukkitPlayer = plugin.getServer().getPlayer(player.getUUID());
        if (bukkitPlayer != null) {
            return bukkitPlayer.hasPermission(permission);
        }
        return false;
    }

    @Override
    public ZonesPlatform getPlatform() {
        return this;
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public PermissionManager getPermissionManager() {
        return plugin.getPermissionManager();
    }

    public ZonesBukkit getPlugin() {
        return plugin;
    }
}
