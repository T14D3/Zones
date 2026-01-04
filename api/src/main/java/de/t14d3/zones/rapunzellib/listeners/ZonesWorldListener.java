package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.PlayerQuitPost;
import de.t14d3.rapunzellib.events.world.WorldLoadPost;
import de.t14d3.zones.Zones;

final class ZonesWorldListener {
    private final Zones zones;

    ZonesWorldListener(Zones zones) {
        this.zones = zones;
    }

    GameEventBus.Subscription register(GameEventBus bus) {
        var s1 = bus.onPost(WorldLoadPost.class, event -> zones.getRegionManager().loadRegions());
        var s2 = bus.onPost(PlayerQuitPost.class,
                event -> zones.getPermissionManager().invalidateSubject(event.uuid()));
        return () -> {
            s1.close();
            s2.close();
        };
    }
}
