package de.t14d3.zones.fabric;


import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesParticleRole;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.visuals.BeaconUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.File;

public class FabricPlatform implements ZonesPlatform {
    private final ZonesFabric mod;

    public SimpleParticleType primary;
    public ParticleOptions secondary;
    public ParticleOptions preview;

    public FabricPlatform(ZonesFabric mod) {
        this.mod = mod;
    }

    @Override
    public File getDataFolder() {
        return mod.getDataFolder();
    }

    @Override
    public PermissionManager createPermissionManager(Zones zones) {
        return new FabricPermissionManager(zones);
    }

    @Override
    public Types getTypes() {
        return mod.getTypes();
    }

    @Override
    public void spawnParticle(ZonesParticleRole role, RBlockPos particleLocation, RPlayer player) {
        ServerPlayer serverPlayer = player.tryHandle(ServerPlayer.class).orElse(null);
        if (serverPlayer == null) return;

        ParticleOptions particle = switch (role) {
            case PRIMARY -> primary;
            case SECONDARY -> secondary;
            case PREVIEW -> preview != null ? preview : secondary;
        };
        serverPlayer.level().sendParticles(
                serverPlayer,
                particle,
                false,
                false,
                particleLocation.x(),
                particleLocation.y(),
                particleLocation.z(),
                1,
                0.00D,
                0.0D,
                0.0D,
                0.0D
        );
    }

    @Override
    public void showBeacon(RPlayer player, RBlockPos location, RWorldRef world, NamedTextColor color) {
        if (location == null) return;
        ServerPlayer nativePlayer = player.tryHandle(ServerPlayer.class).orElse(null);
        if (nativePlayer == null) return;
        ServerLevel nativeWorld = resolveWorld(world, nativePlayer);
        showBeacon(nativePlayer, nativeWorld, location, color);
    }

    private void showBeacon(ServerPlayer player, ServerLevel world, RBlockPos location, NamedTextColor color) {
        RBlockPos adjusted = new RBlockPos(location.x(), location.y() + 1, location.z());
        for (BeaconUtils.BlockChange change : BeaconUtils.createList(adjusted, color)) {
            BlockPos nativePos = new BlockPos(change.x(), change.y(), change.z());
            if (world.getBlockState(nativePos).getLightBlock() < 15) {
                continue;
            }

            Block block = BuiltInRegistries.BLOCK
                    .get(ResourceLocation.withDefaultNamespace(change.blockData().toLowerCase()))
                    .map(ref -> ref.value())
                    .orElse(null);
            if (block == null) {
                continue;
            }

            BlockState state = block.defaultBlockState();
            player.connection.send(new ClientboundBlockUpdatePacket(nativePos, state));
        }
    }

    @Override
    public void removeBeacon(RPlayer player, RWorldRef world, RBlockPos location) {
        if (location == null) return;
        ServerPlayer nativePlayer = player.tryHandle(ServerPlayer.class).orElse(null);
        if (nativePlayer == null) return;
        ServerLevel nativeWorld = resolveWorld(world, nativePlayer);
        removeBeacon(nativePlayer, nativeWorld, location);
    }

    private void removeBeacon(ServerPlayer player, ServerLevel world, RBlockPos location) {
        for (BeaconUtils.BlockChange change : BeaconUtils.resetList(location)) {
            BlockPos nativePos = new BlockPos(change.x(), change.y(), change.z());
            BlockState state = world.getBlockState(nativePos);
            player.connection.send(new ClientboundBlockUpdatePacket(nativePos, state));
        }
    }

    private ServerLevel resolveWorld(RWorldRef world, ServerPlayer fallbackPlayer) {
        if (world == null) return fallbackPlayer.level();
        String id = world.identifier();
        for (ServerLevel level : mod.getServer().getAllLevels()) {
            if (level.dimension().location().toString().equals(id)) {
                return level;
            }
        }
        return fallbackPlayer.level();
    }
}
