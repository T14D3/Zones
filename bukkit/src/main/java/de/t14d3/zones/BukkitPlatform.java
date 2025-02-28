package de.t14d3.zones;

import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BukkitPlatform implements ZonesPlatform {
    private final ZonesBukkit plugin;
    public BukkitAudiences audiences;

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
        org.bukkit.OfflinePlayer player = plugin.getServer().getOfflinePlayer(uuid);
        String name = player.getName();
        if (name == null) {
            name = player.getUniqueId().toString();
        }
        return PlayerRepository.getOrAdd(name, player.getUniqueId());
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

    @Override
    public Types getTypes() {
        return plugin.getTypes();
    }

    @Override
    public Audience getAudience(Player player) {
        return audiences.player(player.getUUID());
    }

    public ZonesBukkit getPlugin() {
        return plugin;
    }

    public BukkitAudiences getAudiences() {
        return audiences;
    }
}
