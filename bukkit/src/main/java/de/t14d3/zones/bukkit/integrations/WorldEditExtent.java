package de.t14d3.zones.integrations;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Set;

public class WorldEditExtent extends AbstractDelegateExtent {
    private final Set<CuboidRegion> mask;

    public WorldEditExtent(Set<CuboidRegion> mask, Extent extent) {
        super(extent);
        this.mask = mask;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean setBlock(BlockVector3 location, BlockStateHolder block) throws WorldEditException {
        if (!WorldEditSession.WorldEditUtils.maskContains(this.mask, location.x(), location.y(), location.z())) {
            return false;
        }
        return super.setBlock(location, block);
    }
}
