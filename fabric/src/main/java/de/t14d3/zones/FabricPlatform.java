package de.t14d3.zones;

import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.kyori.adventure.audience.Audience;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FabricPlatform implements ZonesPlatform {
    private final ZonesFabric mod;

    public FabricPlatform(ZonesFabric mod) {
        this.mod = mod;
        registerCallbacks();
    }

    private void registerCallbacks() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            // Handle block usage events
            return null;
        });
    }

    @Override
    public List<World> getWorlds() {
        List<World> worlds = new ArrayList<>();
        mod.getServer().getWorlds().forEach(world -> worlds.add(World.of(
                world.getRegistryKey().getValue().toString(),
                UUID.nameUUIDFromBytes(world.getRegistryKey().getValue().toString().getBytes())
        )));
        return worlds;
    }

    @Override
    public Player getPlayer(UUID uuid) {
        return null;
    }

    @Override
    public Audience getAudience(Player player) {
        return null;
    }

    @Override
    public boolean hasPermission(Player player, String permission) {
        return false;
    }

    @Override
    public ZonesPlatform getPlatform() {
        return this;
    }

    @Override
    public World getWorld(String world) {
        return ZonesPlatform.super.getWorld(world);
    }

    @Override
    public File getDataFolder() {
        return mod.getDataFolder();
    }

    @Override
    public PermissionManager getPermissionManager() {
        return mod.getPermissionManager();
    }

    @Override
    public Types getTypes() {
        return mod.getTypes();
    }
}
