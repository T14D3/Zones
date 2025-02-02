package de.t14d3.zones.listeners;

import de.t14d3.zones.PermissionManager;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Flag;
import de.t14d3.zones.utils.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.world.StructureGrowEvent;

public class BlockEventListener implements Listener {

    static final String WHO = "universal";
    private final Zones plugin;
    private final RegionManager regionManager;
    private final PermissionManager permissionManager;

    public BlockEventListener(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.permissionManager = plugin.getPermissionManager();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (plugin.getConfig().getBoolean("events.block-physics.enabled", false)) {
            plugin.getServer().getPluginManager().registerEvents(new BlockPhysicsEventListener(), plugin);
        }
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(), WHO, Flags.CREATE, event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        Flag flag =
                switch (event.getBlock().getType()) {
                    case ICE, FROSTED_ICE, SNOW, FIRE -> Flags.DESTROY;
                    default -> Flags.TRANSFORM;
                };
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(),
                WHO,
                flag,
                event.getBlock().getType().name(),
                event.getNewState().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Flag flag =
                switch (event.getNewState().getType()) {
                    case ICE, FROSTED_ICE, SNOW -> Flags.CREATE;
                    default -> Flags.TRANSFORM;
                };
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(),
                WHO,
                flag,
                event.getNewState().getType().name(),
                event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Flag flag =
                switch (event.getBlock().getType()) {
                    case WATER, LAVA -> Flags.SPREAD;
                    default -> Flags.RELOCATE;
                };
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(),
                WHO,
                flag,
                event.getBlock().getType().name(),
                event.getToBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(),
                WHO,
                Flags.SPREAD,
                event.getBlock().getType().name(),
                event.getNewState().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSculkSpread(SculkBloomEvent event) {
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(), WHO, Flags.SPREAD, event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        event
                .getBlocks()
                .removeIf(
                        state ->
                                !permissionManager.checkAction(
                                        state.getLocation(), WHO, Flags.CREATE, state.getType().name()));
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!permissionManager.checkAction(
                event.getBlock().getLocation(), WHO, Flags.DESTROY, event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    // Separate Listener class for BlockPhysicsEvent, so we can register it selectively
    // (High-Frequency event)
    private class BlockPhysicsEventListener implements Listener {
        @EventHandler
        public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
            if (!permissionManager.checkAction(
                    event.getBlock().getLocation(), WHO, Flags.PHYSICS, event.getBlock().getType().name())) {
                event.setCancelled(true);
            }
        }
    }
}
