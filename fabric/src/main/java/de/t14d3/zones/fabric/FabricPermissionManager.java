package de.t14d3.zones.fabric;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.PermissionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class FabricPermissionManager extends PermissionManager {
    public FabricPermissionManager(Zones zones) {
        super(zones);
    }

    public boolean checkAction(BlockPos pos, net.minecraft.world.level.Level nativeWorld, Player nativePlayer, Flag flag) {
        return checkAction(pos, nativeWorld, nativePlayer,
                nativePlayer.getMainHandItem().getItem().getDescriptionId(), flag);
    }

    public boolean checkAction(BlockPos pos, net.minecraft.world.level.Level nativeWorld, Player nativePlayer, String type, Flag flag) {
        RBlockPos location = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
        String worldKey = nativeWorld.dimension().location().toString();
        RWorldRef world = new RWorldRef(null, worldKey);
        return super.checkAction(location, world, nativePlayer.getStringUUID(), flag, type);
    }
}
