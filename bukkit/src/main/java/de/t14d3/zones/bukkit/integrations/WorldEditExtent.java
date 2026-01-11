package de.t14d3.zones.bukkit.integrations;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.TypeKeys;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class WorldEditExtent extends AbstractDelegateExtent {
    private final Set<CuboidRegion> mask;
    private final Zones zones;
    private final UUID playerUuid;
    private final RWorldRef world;
    private final boolean enforceMask;
    private final boolean bypassClaimed;

    public WorldEditExtent(Set<CuboidRegion> mask, Extent extent, Zones zones, Player player) {
        super(extent);
        this.mask = mask;
        this.zones = zones;

        if (player != null) {
            this.playerUuid = player.getUniqueId();
            this.world = new RWorldRef(player.getWorld().getName(), player.getWorld().getKey().toString());
            this.enforceMask = !player.hasPermission("zones.bypass.unclaimed");
            this.bypassClaimed = player.hasPermission("zones.bypass.claimed");
        } else {
            this.playerUuid = null;
            this.world = null;
            this.enforceMask = true;
            this.bypassClaimed = true;
        }
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        if (bypassClaimed) return super.setBlock(location, block);
        if (enforceMask && (mask == null || mask.isEmpty()
                || !WorldEditSession.WorldEditUtils.maskContains(this.mask, location.x(), location.y(),
                location.z()))) {
            return false;
        }
        if (zones == null || world == null || playerUuid == null) return super.setBlock(location, block);

        String typeKey = block.getBlockType().id();
        boolean isAir = TypeKeys.normalize(typeKey).endsWith("air");

        RBlockPos pos = new RBlockPos(location.x(), location.y(), location.z());
        if (!zones.getPermissionManager()
                .checkAction(pos, world, playerUuid, isAir ? Flags.BREAK : Flags.PLACE, typeKey)) {
            return false;
        }
        return super.setBlock(location, block);
    }
}
