package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.entity.AttackEntityPre;
import de.t14d3.rapunzellib.events.entity.EntityHurtPre;
import de.t14d3.rapunzellib.events.entity.EntitySpawnPre;
import de.t14d3.rapunzellib.events.entity.InteractEntityPre;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.FlagContext;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.rapunzellib.ZonesActionbar;
import de.t14d3.zones.utils.TypeKeys;

final class ZonesEntityListener {
    private final Zones zones;

    private static final Flag[] INTERACT_ENTITY = {Flags.INTERACT, Flags.ENTITY};
    private static final Flag[] INTERACT_ENTITY_CONTAINER = {Flags.INTERACT, Flags.ENTITY, Flags.CONTAINER};
    private static final Flag[] DAMAGE_ENTITY = {Flags.DAMAGE, Flags.ENTITY};

    ZonesEntityListener(Zones zones) {
        this.zones = zones;
    }

    GameEventBus.Subscription register(GameEventBus bus) {
        var s1 = bus.onPre(InteractEntityPre.class, event -> {
            RPlayer player = event.player();
            if (player.hasPermission("zones.bypass.claimed")) return;

            String type = TypeKeys.normalize(event.entityTypeKey());
            Flag[] required = type.equals("armor_stand") ? INTERACT_ENTITY_CONTAINER : INTERACT_ENTITY;
            if (!zones.getPermissionManager().checkActions(event.pos(), event.world(), player.uuid(), type, required)) {
                event.deny();
                ZonesActionbar.sendDenied(zones, player, event.pos(), event.world(), required, type);
            }
        });
        var s2 = bus.onPre(AttackEntityPre.class, event -> {
            RPlayer player = event.player();
            if (player.hasPermission("zones.bypass.claimed")) return;

            String type = TypeKeys.normalize(event.entityTypeKey());
            if (!zones.getPermissionManager()
                    .checkActions(event.pos(), event.world(), player.uuid(), type, DAMAGE_ENTITY)) {
                event.deny();
                ZonesActionbar.sendDenied(zones, player, event.pos(), event.world(), DAMAGE_ENTITY, type);
            }
        });
        var s3 = bus.onPre(EntitySpawnPre.class, event -> {
            String type = TypeKeys.normalize(event.entityTypeKey());
            if (!zones.getPermissionManager().checkAction(event.pos(), event.world(), Flags.SPAWN, type,
                    FlagContext.none().withReason(event.reason()))) {
                event.deny();
            }
        });
        var s4 = bus.onPre(EntityHurtPre.class, event -> {
            String type = TypeKeys.normalize(event.entityTypeKey());
            if (!zones.getPermissionManager().checkAction(event.pos(), event.world(), Flags.HURT, type,
                    FlagContext.none().withDamageType(event.damageTypeKey()))) {
                event.deny();
            }
        });
        return () -> {
            s1.close();
            s2.close();
            s3.close();
            s4.close();
        };
    }
}
