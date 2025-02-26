package de.t14d3.zones;

import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.atomic.AtomicReference;

public class FabricPermissionManager extends PermissionManager {
    private final Zones zones;
    public FabricPermissionManager(Zones zones) {
        super(zones);
        this.zones = zones;
        register();
    }

    public boolean checkAction(BlockPos pos, net.minecraft.world.World nativeWorld, PlayerEntity nativePlayer, Flag flag) {
        return checkAction(pos, nativeWorld, nativePlayer,
                nativePlayer.getMainHandStack().getItem().getTranslationKey(), flag);
    }

    public boolean checkAction(BlockPos pos, net.minecraft.world.World nativeWorld, PlayerEntity nativePlayer, String type, Flag flag) {
        type = type.replace("minecraft:", "");
        BlockLocation location = BlockLocation.of(pos.getX(), pos.getY(), pos.getZ());
        World world = ((FabricPlatform) zones.getPlatform()).getWorld(nativeWorld);
        return super.checkAction(location, world, nativePlayer.getUuidAsString(), flag, type);
    }

    public void register() {
        PermissionCheckEvent.EVENT.register((source, permission) -> {
            if (!permission.startsWith("zones.")) {
                return TriState.DEFAULT;
            }
            AtomicReference<TriState> result = new AtomicReference<>(TriState.DEFAULT);
            this.permissionMap.stream().filter(p -> p.getName().equals(permission)).findFirst().ifPresent(p -> {
                if (source.hasPermissionLevel(p.getLevel())) {
                    result.set(TriState.TRUE);
                }
            });
            return result.get();
        });
    }
}
