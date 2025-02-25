package de.t14d3.zones;

import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.permissions.flags.Flags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

public class FabricPermissionManager extends PermissionManager {
    private final Zones zones;
    public FabricPermissionManager(Zones zones) {
        super(zones);
        this.zones = zones;
    }

    public boolean checkAction(BlockHitResult hitResult, net.minecraft.world.World nativeWorld, PlayerEntity nativePlayer) {
        BlockLocation location = BlockLocation.of(hitResult.getBlockPos().getX(), hitResult.getBlockPos().getY(),
                hitResult.getBlockPos().getZ());
        World world = ((FabricPlatform) zones.getPlatform()).getWorld(nativeWorld);
        String type = nativePlayer.getActiveItem().getItem().getTranslationKey();
        return super.checkAction(location, world, Flags.PLACE, type);
    }
}
