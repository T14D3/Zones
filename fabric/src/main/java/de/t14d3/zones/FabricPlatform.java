package de.t14d3.zones;

import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.PlayerRepository;
import de.t14d3.zones.utils.Types;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.kyori.adventure.audience.Audience;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FabricPlatform implements ZonesPlatform {
    private final ZonesFabric mod;
    private FabricPermissionManager permissionManager;

    public FabricPlatform(ZonesFabric mod) {
        this.mod = mod;
        this.permissionManager = (FabricPermissionManager) mod.getPermissionManager();
        registerCallbacks();
    }

    private void registerCallbacks() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                permissionManager.checkAction(hitResult, world, player) ? ActionResult.PASS : ActionResult.FAIL);
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
        String name = mod.getServer().getUserCache().getByUuid(uuid).get().getName();
        return PlayerRepository.getOrAdd(name, uuid);
    }

    public ServerPlayerEntity getNativePlayer(Player player) {
        return mod.getServer().getPlayerManager().getPlayer(player.getUUID());
    }

    @Override
    public Audience getAudience(Player player) {
        return getNativePlayer(player);
    }

    @Override
    public boolean hasPermission(Player player, String permission) {
        return Permissions.check(player.getUUID(), permission).join();
    }

    @Override
    public ZonesPlatform getPlatform() {
        return this;
    }


    public World getWorld(net.minecraft.world.World nativeWorld) {
        return getWorld(nativeWorld.getRegistryKey().getValue().toString());
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
