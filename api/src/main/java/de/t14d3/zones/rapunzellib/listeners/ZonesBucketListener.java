package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.item.BucketEmptyPre;
import de.t14d3.rapunzellib.events.item.BucketEntityPre;
import de.t14d3.rapunzellib.events.item.BucketFillPre;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.TypeKeys;

final class ZonesBucketListener {
    private final Zones zones;

    ZonesBucketListener(Zones zones) {
        this.zones = zones;
    }

    GameEventBus.Subscription register(GameEventBus bus) {
        var s1 = bus.onPre(BucketFillPre.class, event -> {
            RPlayer player = event.player();
            if (player.hasPermission("zones.bypass.claimed")) return;

            String type = TypeKeys.normalize(event.blockTypeKey());
            if (!zones.getPermissionManager()
                    .checkAction(event.pos(), event.world(), player.uuid(), Flags.BREAK, type)) {
                event.deny();
            }
        });
        var s2 = bus.onPre(BucketEmptyPre.class, event -> {
            RPlayer player = event.player();
            if (player.hasPermission("zones.bypass.claimed")) return;

            String raw = TypeKeys.normalize(event.bucketTypeKey());
            String type = switch (raw) {
                case "water_bucket" -> "water";
                case "lava_bucket" -> "lava";
                case "powder_snow_bucket" -> "powder_snow";
                default -> raw;
            };

            if (!zones.getPermissionManager()
                    .checkAction(event.pos(), event.world(), player.uuid(), Flags.PLACE, type)) {
                event.deny();
            }
        });
        var s3 = bus.onPre(BucketEntityPre.class, event -> {
            RPlayer player = event.player();
            if (player.hasPermission("zones.bypass.claimed")) return;

            String type = TypeKeys.normalize(event.entityTypeKey());
            if (!zones.getPermissionManager()
                    .checkAction(event.pos(), event.world(), player.uuid(), Flags.ENTITY, type)) {
                event.deny();
            }
        });
        return () -> {
            s1.close();
            s2.close();
            s3.close();
        };
    }
}
