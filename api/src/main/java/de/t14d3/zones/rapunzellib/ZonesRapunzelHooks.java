package de.t14d3.zones.rapunzellib;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.zones.Zones;
import de.t14d3.zones.rapunzellib.listeners.ZonesEventListeners;

import java.util.ArrayList;
import java.util.List;

/**
 * Installs Zones listeners onto RapunzelLib's game event bus.
 *
 * <p>This is an optional integration layer for servers using RapunzelLib.</p>
 */
public final class ZonesRapunzelHooks implements AutoCloseable {
    private final List<GameEventBus.Subscription> subscriptions = new ArrayList<>();

    private ZonesRapunzelHooks() {
    }

    /**
     * Installs Zones hooks once and registers the instance as a Rapunzel service.
     *
     * @param zones zones instance
     * @return the installed hooks instance (existing one if already installed)
     */
    public static ZonesRapunzelHooks install(Zones zones) {
        var ctx = Rapunzel.context();
        var already = ctx.services().find(ZonesRapunzelHooks.class).orElse(null);
        if (already != null) return already;

        ZonesRapunzelHooks hooks = new ZonesRapunzelHooks();

        GameEventBus bus = GameEvents.bus();
        hooks.subscriptions.add(ZonesEventListeners.registerAll(zones, bus));

        ctx.services().register(ZonesRapunzelHooks.class, hooks);
        return hooks;
    }

    /**
     * Unregisters all installed subscriptions.
     */
    @Override
    public void close() {
        for (var s : subscriptions) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
        subscriptions.clear();
    }
}
