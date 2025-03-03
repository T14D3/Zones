package de.t14d3.zones.bukkit;

import com.destroystokyo.paper.ParticleBuilder;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.visuals.BeaconUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BukkitPlatform implements ZonesPlatform {
    private final ZonesBukkit plugin;
    private final Particle primary;
    private final Particle secondary;
    public BukkitAudiences audiences;

    public BukkitPlatform(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.primary = Particle.valueOf(plugin.getConfig().getString("selection-particles.primary", "WAX_OFF"));
        this.secondary = Particle.valueOf(plugin.getConfig().getString("selection-particles.secondary", "WAX_ON"));
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

    public org.bukkit.entity.Player getNativePlayer(Player player) {
        return plugin.getServer().getPlayer(player.getUUID());
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

    @Override
    public World getWorld(Player player) {
        return plugin.getPlatform().getWorld(plugin.getServer().getPlayer(player.getUUID()).getWorld().getName());
    }

    @Override
    public BlockLocation getLocation(Player player) {
        return BlockLocation.of(
                plugin.getServer().getPlayer(player.getUUID()).getLocation().toVector().toBlockVector());
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @Nullable String getMetadata(Player player, String key) {
        try {
            return plugin.getServer().getPlayer(player.getUUID()).getMetadata(key).stream().filter(value ->
                    value.getOwningPlugin().equals(plugin)).findFirst().map(MetadataValue::asString).orElse(null);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void setMetadata(Player player, String key, String value) {
        try {
            plugin.getServer().getPlayer(player.getUUID()).removeMetadata(key, plugin);
            plugin.getServer().getPlayer(player.getUUID()).setMetadata(key, new FixedMetadataValue(plugin, value));
        } catch (NullPointerException ignored) {
        }
    }

    @Override
    public void spawnParticle(int type, BlockLocation particleLocation, Player player) {
        org.bukkit.entity.Player bukkitPlayer = plugin.getServer().getPlayer(player.getUUID());
        org.bukkit.Location bukkitLocation = particleLocation.toLocation(Bukkit.getWorld(player.getWorld().getUID()));
        ParticleBuilder particleBuilder = new ParticleBuilder(type == 1 ? primary : secondary);
        particleBuilder.location(bukkitLocation);
        particleBuilder.receivers(bukkitPlayer);
        particleBuilder.count(1);
        particleBuilder.extra(0);
        particleBuilder.force(true);
        particleBuilder.spawn();
    }

    @Override
    public void showBeacon(de.t14d3.zones.objects.Player player, de.t14d3.zones.objects.BlockLocation location, de.t14d3.zones.objects.World world, NamedTextColor color) {
        showBeacon(((BukkitPlatform) plugin.getPlatform()).getNativePlayer(player),
                location.toLocation(plugin.getServer().getWorld(world.getUID())), color);
    }

    public void showBeacon(org.bukkit.entity.Player player, Location location, NamedTextColor color) {
        for (BeaconUtils.BlockChange change : BeaconUtils.createList(BlockLocation.of(location), color)) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                BlockData data = Bukkit.createBlockData(Material.valueOf(change.getBlockData()));
                player.sendBlockChange(location, data);
            });
        }
    }

    @Override
    public void removeBeacon(de.t14d3.zones.objects.Player player, de.t14d3.zones.objects.World world, de.t14d3.zones.objects.BlockLocation location) {
        removeBeacon(((BukkitPlatform) plugin.getPlatform()).getNativePlayer(player),
                location.toLocation(plugin.getServer().getWorld(world.getUID())));
    }

    public void removeBeacon(org.bukkit.entity.Player player, Location location) {
        for (BeaconUtils.BlockChange change : BeaconUtils.resetList(BlockLocation.of(location))) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                BlockData data = location.getBlock().getBlockData();
                player.sendBlockChange(location, data);
            });
        }
    }
}
