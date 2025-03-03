package de.t14d3.zones.fabric;

import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.visuals.BeaconUtils;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

public class FabricPlatform implements ZonesPlatform {
    private final ZonesFabric mod;
    private FabricPermissionManager permissionManager;
    private List<World> worlds;
    public SimpleParticleType primary;
    public ParticleOptions secondary;

    public FabricPlatform(ZonesFabric mod) {
        this.mod = mod;

    }

    public void loadWorlds(MinecraftServer server) {
        worlds = new ArrayList<>();
        server.getAllLevels().forEach(world -> worlds.add(World.of(
                world.dimension().location().toString(),
                UUID.nameUUIDFromBytes(world.dimension().location().toString().getBytes())
        )));
    }

    @Override
    public List<World> getWorlds() {
        return worlds;
    }

    @Override
    public Player getPlayer(UUID uuid) {
        String name = mod.getServer().getProfileCache().get(uuid).get().getName();
        return PlayerRepository.getOrAdd(name, uuid);
    }

    public @Nullable Player getPlayer(String name) {
        return PlayerRepository.getPlayers().stream().filter(player -> player.getName().equals(name)).findFirst()
                .orElse(null);
    }

    public ServerPlayer getNativePlayer(Player player) {
        return mod.getServer().getPlayerList().getPlayer(player.getUUID());
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

    public World getWorld(net.minecraft.world.level.Level nativeWorld) {
        return getWorld(nativeWorld.dimension().location().toString());
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

    public ZonesFabric getMod() {
        return mod;
    }

    @Override
    public World getWorld(Player player) {
        return getWorld(getNativePlayer(player).level());
    }

    public ServerLevel getNativeWorld(World world) {
        var ref = new Object() {
            ServerLevel level = null;
        };
        mod.getServer().getAllLevels().forEach(serverLevel -> {
            if (UUID.nameUUIDFromBytes(serverLevel.dimension().location().toString().getBytes())
                    .equals(world.getUID())) {
                ref.level = serverLevel;
            }
        });
        return ref.level;
    }

    @Override
    public BlockLocation getLocation(Player player) {
        return BlockLocation.of(getNativePlayer(player).getBlockX(), getNativePlayer(player).getBlockY(),
                getNativePlayer(player).getBlockZ());
    }

    @Override
    public String getMetadata(Player player, String key) {
        // Format: zones:key:value
        return getNativePlayer(player).getTags().stream().filter(tag ->
                tag.startsWith("zones:" + key)).findFirst().map(tag ->
                tag.substring(tag.indexOf(':') + 1).substring(tag.indexOf(':') + 1)).orElse(null);
    }

    @Override
    public void setMetadata(Player player, String key, String value) {
        getNativePlayer(player).getTags().removeIf(tag ->
                tag.startsWith("zones:" + key));
        getNativePlayer(player).getTags().add("zones:" + key + ":" + value);
    }

    @Override
    public void spawnParticle(int type, BlockLocation particleLocation, Player player) {
        ServerPlayer serverPlayer = getNativePlayer(player);
        serverPlayer.serverLevel()
                .sendParticles(serverPlayer, (type == 1 ? primary : secondary), false, false, particleLocation.getX(),
                        particleLocation.getY(), particleLocation.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void showBeacon(Player player, BlockLocation location, World world, NamedTextColor color) {
        showBeacon(mod.getPlatform().getNativePlayer(player), location, color);
    }

    private void showBeacon(ServerPlayer player, BlockLocation location, NamedTextColor color) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Registry<Block> blocks = mod.getServer().registryAccess().lookupOrThrow(Registries.BLOCK);
            for (BeaconUtils.BlockChange change : BeaconUtils.createList(location.setY(location.getY() + 1), color)) {
                BlockPos nativePos = new BlockPos(change.getX(), change.getY(), change.getZ());
                if (player.serverLevel().getBlockState(nativePos).getLightBlock() < 15) {
                    continue;
                }
                BlockState state = blocks.get(
                                ResourceLocation.withDefaultNamespace(change.getBlockData().toLowerCase()))
                        .get().value().defaultBlockState();
                player.connection.send(new ClientboundBlockUpdatePacket(nativePos, state));
            }
        });
    }

    @Override
    public void removeBeacon(Player player, World world, BlockLocation location) {
        if (location == null) return;
        removeBeacon(mod.getPlatform().getNativePlayer(player), world, location);
    }

    private void removeBeacon(ServerPlayer nativePlayer, World world, BlockLocation location) {
        for (BeaconUtils.BlockChange change : BeaconUtils.resetList(location)) {
            BlockPos nativePos = new BlockPos(change.getX(), change.getY(), change.getZ());
            BlockState state = getNativeWorld(world).getBlockState(nativePos);
            nativePlayer.connection.send(new ClientboundBlockUpdatePacket(nativePos, state));
        }
    }
}
