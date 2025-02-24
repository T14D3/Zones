package de.t14d3.zones.listeners;

import de.t14d3.zones.*;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.world.StructureGrowEvent;

public class BlockEventListener implements Listener {

    private final Zones zones;
    private final ZonesBukkit plugin;
    private final RegionManager regionManager;
    private final BukkitPermissionManager permissionManager;

    public BlockEventListener(Zones zones) {
        this.zones = zones;
        this.regionManager = zones.getRegionManager();
        this.plugin = ((BukkitPlatform) zones.getPlatform()).getPlugin();
        this.permissionManager = plugin.getPermissionManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (zones.getConfig().getBoolean("events.block-physics.enabled", false)) {
            plugin.getServer().getPluginManager().registerEvents(new BlockPhysicsEventListener(), plugin);
        }
    }

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.CREATE,
                event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        Flag flag = switch (event.getBlock().getType()) {
            case ICE, FROSTED_ICE, SNOW, FIRE -> Flags.DESTROY;
            default -> Flags.TRANSFORM;
        };
        if (!permissionManager.checkAction(event.getBlock().getLocation(), flag, event.getBlock().getType().name(),
                event.getNewState().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Flag flag = switch (event.getNewState().getType()) {
            case ICE, FROSTED_ICE, SNOW -> Flags.CREATE;
            default -> Flags.TRANSFORM;
        };
        if (!permissionManager.checkAction(event.getBlock().getLocation(), flag, event.getNewState().getType().name(),
                event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Flag flag = switch (event.getBlock().getType()) {
            case WATER, LAVA -> Flags.SPREAD;
            default -> Flags.RELOCATE;
        };
        if (!permissionManager.checkAction(event.getBlock().getLocation(), flag, event.getBlock().getType().name(),
                event.getToBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.SPREAD,
                event.getBlock().getType().name(), event.getNewState().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSculkSpread(SculkBloomEvent event) {
        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.SPREAD,
                event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onStructureGrow(StructureGrowEvent event) {
        event.getBlocks().removeIf(
                state -> !permissionManager.checkAction(state.getLocation(), Flags.CREATE, state.getType().name()));
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.DESTROY,
                event.getBlock().getType().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.RELOCATE,
                event.getBlock().getType().name())) {
            event.setCancelled(true);
            return;
        }
        for (Block block : event.getBlocks()) {
            if (!permissionManager.checkAction(block.getLocation(), Flags.RELOCATE,
                    event.getBlock().getType().name())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.RELOCATE,
                event.getBlock().getType().name())) {
            event.setCancelled(true);
            return;
        }
        for (Block block : event.getBlocks()) {
            if (!permissionManager.checkAction(block.getLocation(), Flags.RELOCATE,
                    event.getBlock().getType().name())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    // Separate Listener class for BlockPhysicsEvent, so we can register it selectively
    // (High-Frequency event)
    private class BlockPhysicsEventListener implements Listener {
        @EventHandler
        public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
            if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.PHYSICS,
                    event.getBlock().getType().name())) {
                event.setCancelled(true);
            }
        }
    }
}
