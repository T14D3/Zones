package de.t14d3.zones.bukkit;

import com.destroystokyo.paper.ParticleBuilder;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.visuals.BeaconUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;

import java.io.File;

public class BukkitPlatform implements ZonesPlatform {
    private final ZonesBukkit plugin;
    private final Particle primary;
    private final Particle secondary;

    public BukkitPlatform(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.primary = Particle.valueOf(plugin.getConfig().getString("visuals.particles.primary", "WAX_OFF"));
        this.secondary = Particle.valueOf(plugin.getConfig().getString("visuals.particles.secondary", "WAX_ON"));
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public PermissionManager createPermissionManager(Zones zones) {
        return new BukkitPermissionManager(zones);
    }

    @Override
    public Types getTypes() {
        return plugin.getTypes();
    }

    public ZonesBukkit getPlugin() {
        return plugin;
    }

    @Override
    public void spawnParticle(int type, RBlockPos particleLocation, RPlayer player) {
        org.bukkit.entity.Player bukkitPlayer = player.tryHandle(org.bukkit.entity.Player.class).orElse(null);
        if (bukkitPlayer == null) return;
        org.bukkit.Location bukkitLocation = new Location(bukkitPlayer.getWorld(), particleLocation.x(),
                particleLocation.y(), particleLocation.z());
        ParticleBuilder particleBuilder = new ParticleBuilder(type == 1 ? primary : secondary);
        particleBuilder.location(bukkitLocation);
        particleBuilder.receivers(bukkitPlayer);
        particleBuilder.count(1);
        particleBuilder.extra(0);
        particleBuilder.force(true);
        particleBuilder.spawn();
    }

    @Override
    public void showBeacon(RPlayer player, RBlockPos location, RWorldRef world, NamedTextColor color) {
        if (location == null) return;
        org.bukkit.entity.Player bukkitPlayer = player.tryHandle(org.bukkit.entity.Player.class).orElse(null);
        if (bukkitPlayer == null) return;
        org.bukkit.World bukkitWorld = resolveWorld(world, bukkitPlayer);
        showBeacon(bukkitPlayer, new Location(bukkitWorld, location.x(), location.y(), location.z()), color);
    }

    public void showBeacon(org.bukkit.entity.Player player, Location location, NamedTextColor color) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            RBlockPos pos = new RBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            for (BeaconUtils.BlockChange change : BeaconUtils.createList(pos, color)) {
                BlockData data = Bukkit.createBlockData(Material.valueOf(change.blockData()));
                Location loc = new Location(location.getWorld(), change.x(), change.y(), change.z());
                if (!loc.getBlock().getType().isOccluding()) {
                    continue;
                }
                player.sendBlockChange(loc, data);
            }
        });
    }

    @Override
    public void removeBeacon(RPlayer player, RWorldRef world, RBlockPos location) {
        if (location == null) return;
        org.bukkit.entity.Player bukkitPlayer = player.tryHandle(org.bukkit.entity.Player.class).orElse(null);
        if (bukkitPlayer == null) return;
        org.bukkit.World bukkitWorld = resolveWorld(world, bukkitPlayer);
        removeBeacon(bukkitPlayer, new Location(bukkitWorld, location.x(), location.y(), location.z()));
    }

    public void removeBeacon(org.bukkit.entity.Player player, Location location) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            RBlockPos pos = new RBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            for (BeaconUtils.BlockChange change : BeaconUtils.resetList(pos)) {
                Location loc = new Location(location.getWorld(), change.x(), change.y(), change.z());
                BlockData data = loc.getBlock().getBlockData();
                player.sendBlockChange(loc, data);
            }
        });
    }

    private static org.bukkit.World resolveWorld(RWorldRef world, org.bukkit.entity.Player fallbackPlayer) {
        if (world == null) return fallbackPlayer.getWorld();
        if (world.key() != null) {
            try {
                var key = org.bukkit.NamespacedKey.fromString(world.key());
                if (key != null) {
                    var byKey = Bukkit.getWorld(key);
                    if (byKey != null) return byKey;
                }
            } catch (Exception ignored) {
            }
        }
        if (world.name() != null) {
            var byName = Bukkit.getWorld(world.name());
            if (byName != null) return byName;
        }
        var byIdentifier = Bukkit.getWorld(world.identifier());
        if (byIdentifier != null) return byIdentifier;
        return fallbackPlayer.getWorld();
    }
}
