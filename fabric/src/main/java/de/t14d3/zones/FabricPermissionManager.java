package de.t14d3.zones;

import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class FabricPermissionManager extends PermissionManager {
    private final Zones zones;
    public FabricPermissionManager(Zones zones) {
        super(zones);
        this.zones = zones;
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
}
