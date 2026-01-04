package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.zones.Zones;

/**
 * Installs all Zones listeners onto a {@link GameEventBus}.
 */
public final class ZonesEventListeners {
    private ZonesEventListeners() {
    }

    /**
     * Registers all Zones listeners and returns a subscription that un-registers them.
     *
     * @param zones zones instance
     * @param bus   event bus to register listeners on
     */
    public static GameEventBus.Subscription registerAll(Zones zones, GameEventBus bus) {
        var selection = new ZonesSelectionListener(zones).register(bus);
        var blocks = new ZonesBlockListener(zones).register(bus);
        var entities = new ZonesEntityListener(zones).register(bus);
        var explosions = new ZonesExplosionListener(zones).register(bus);
        var buckets = new ZonesBucketListener(zones).register(bus);
        var world = new ZonesWorldListener(zones).register(bus);

        return () -> {
            selection.close();
            blocks.close();
            entities.close();
            explosions.close();
            buckets.close();
            world.close();
        };
    }
}
