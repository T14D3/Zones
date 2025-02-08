package de.t14d3.zones.listeners;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.Flags;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.DebugLoggerManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ExplosivesListener {
    private final Zones plugin;
    private final PermissionManager permissionManager;

    private final int limit;
    private final boolean limitExceededCancel;
    private final AtomicInteger explosionCount = new AtomicInteger(0);
    private final DebugLoggerManager logger;

    public ExplosivesListener(Zones plugin) {
        this.plugin = plugin;
        this.permissionManager = plugin.getPermissionManager();
        this.logger = plugin.getDebugLogger();

        String explosionMode = plugin.getConfig().getString("events.explosion.explosion-mode", "ALL").toUpperCase();
        limit = plugin.getConfig().getInt("events.explosion.limit", 100);
        limitExceededCancel = plugin.getConfig().getString("events.explosion.limit-exceeded-action", "CANCEL")
                .equalsIgnoreCase("CANCEL");
        limitHandler();

        switch (plugin.getConfig().getString("events.explosion.mode", "ALL").toUpperCase()) {
            case "ALL":
                registerExplosionListener(explosionMode);
                plugin.getServer().getPluginManager().registerEvents(new IgnitionListener(), plugin);
                break;
            case "IGNITION":
                registerExplosionListener(explosionMode);
                break;
            case "EXPLOSION":
                plugin.getServer().getPluginManager().registerEvents(new IgnitionListener(), plugin);
                break;
            case "NONE":
                break;
        }
    }

    private void limitHandler() {
        // Reset explosion counter every tick
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            explosionCount.set(0);
        }, 1L, 1L);
    }

    private boolean limitExceeded() {
        return explosionCount.incrementAndGet() > limit;
    }

    private void registerExplosionListener(String explosionMode) {
        switch (explosionMode.toUpperCase()) {
            case "ANY":
                class ExplosionListenerAny implements Listener {
                    @EventHandler
                    public void onBlockExplode(BlockExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        for (Block block : event.blockList()) {
                            if (!permissionManager.checkAction(block.getLocation(), Flags.EXPLOSION,
                                    block.getType().name(), event.getExplodedBlockState().getType())) {
                                event.setCancelled(true);
                                break;
                            }
                        }
                    }

                    @EventHandler
                    public void onEntityExplode(EntityExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        for (Block block : event.blockList()) {
                            if (!permissionManager.checkAction(block.getLocation(), Flags.EXPLOSION,
                                    block.getType().name(), event.getEntityType())) {
                                event.setCancelled(true);
                                break;
                            }
                        }
                    }
                }
                plugin.getServer().getPluginManager().registerEvents(new ExplosionListenerAny(), plugin);
                break;
            case "ALL":
                class ExplosionListenerAll implements Listener {
                    @EventHandler
                    public void onBlockExplode(BlockExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        event.blockList().removeIf(
                                block -> !permissionManager.checkAction(block.getLocation(), Flags.EXPLOSION,
                                        block.getType().name(), event.getExplodedBlockState().getType()));
                    }

                    @EventHandler
                    public void onEntityExplode(EntityExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        event.blockList().removeIf(
                                block -> !permissionManager.checkAction(block.getLocation(), Flags.EXPLOSION,
                                        block.getType().name(), event.getEntityType().name()));
                    }
                }
                plugin.getServer().getPluginManager().registerEvents(new ExplosionListenerAll(), plugin);
                return;
            case "REGION":
                class ExplosionListenerRegion implements Listener {
                    @EventHandler
                    public void onBlockExplode(BlockExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        if (permissionManager.checkAction(event.getBlock().getLocation(), Flags.EXPLOSION,
                                event.getBlock().getType().name(), event.getExplodedBlockState().getType())) {
                            Region region = plugin.getRegionManager()
                                    .getEffectiveRegionAt(event.getBlock().getLocation());
                            event.blockList().removeIf(block -> !Objects.equals(
                                    plugin.getRegionManager().getEffectiveRegionAt(block.getLocation()), region));
                        } else {
                            event.setCancelled(true);
                        }
                    }

                    @EventHandler
                    public void onEntityExplode(EntityExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        if (permissionManager.checkAction(event.getLocation(), Flags.EXPLOSION,
                                event.getEntityType().name(), event.getEntityType())) {
                            Region region = plugin.getRegionManager()
                                    .getEffectiveRegionAt(event.getLocation());
                            event.blockList().removeIf(block -> !Objects.equals(
                                    plugin.getRegionManager().getEffectiveRegionAt(block.getLocation()), region));
                        } else {
                            event.setCancelled(true);
                        }
                    }
                }
                plugin.getServer().getPluginManager().registerEvents(new ExplosionListenerRegion(), plugin);
                return;
            case "SOURCE":
                class ExplosionListenerBlock implements Listener {
                    @EventHandler
                    public void onBlockExplode(BlockExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.EXPLOSION,
                                event.getExplodedBlockState().getType().name())) {
                            event.setCancelled(true);
                        }
                    }

                    @EventHandler
                    public void onEntityExplode(EntityExplodeEvent event) {
                        if (limitExceeded()) {
                            if (limitExceededCancel) {
                                event.setCancelled(true);
                            }
                            return;
                        }
                        if (!permissionManager.checkAction(event.getLocation(), Flags.EXPLOSION,
                                event.getEntityType().name())) {
                            event.setCancelled(true);
                        }
                    }
                }
                plugin.getServer().getPluginManager().registerEvents(new ExplosionListenerBlock(), plugin);
                return;
            default:
        }
    }

    private class IgnitionListener implements Listener {
        @EventHandler
        public void onTNTPrime(TNTPrimeEvent event) {
            if (event.getPrimingEntity() instanceof org.bukkit.entity.Player player) {
                if (!permissionManager.checkAction(event.getBlock().getLocation(), player.getUniqueId(), Flags.IGNITE,
                        event.getBlock().getType().name())) {
                    event.setCancelled(true);
                }
            } else {
                if (!permissionManager.checkAction(event.getBlock().getLocation(), Flags.IGNITE,
                        event.getBlock().getType().name(), event.getCause())) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
