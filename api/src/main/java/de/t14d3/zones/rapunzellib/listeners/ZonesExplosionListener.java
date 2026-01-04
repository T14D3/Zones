package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.world.ExplosionPre;
import de.t14d3.rapunzellib.events.world.TntPrimePre;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.flags.FlagContext;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.TypeKeys;

final class ZonesExplosionListener {
    private final Zones zones;

    ZonesExplosionListener(Zones zones) {
        this.zones = zones;
    }

    GameEventBus.Subscription register(GameEventBus bus) {
        var s1 = bus.onPre(TntPrimePre.class, event -> {
            String type = TypeKeys.normalize(event.blockTypeKey());
            if (event.player().isPresent()) {
                RPlayer player = event.player().get();
                if (player.hasPermission("zones.bypass.claimed")) return;
                if (!zones.getPermissionManager()
                        .checkAction(event.pos(), event.world(), player.uuid(), Flags.IGNITE, type)) {
                    event.deny();
                }
            } else {
                if (!zones.getPermissionManager().checkAction(event.pos(), event.world(), Flags.IGNITE, type,
                        FlagContext.defaultDecision(true).withCause(String.valueOf(event.cause())))) {
                    event.deny();
                }
            }
        });
        var s2 = bus.onPre(ExplosionPre.class, event -> {
            String type = TypeKeys.normalize(event.sourceTypeKey());
            if (!zones.getPermissionManager().checkAction(event.origin(), event.world(), Flags.EXPLOSION, type)) {
                event.deny();
            }
        });
        return () -> {
            s1.close();
            s2.close();
        };
    }
}
