package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.block.BlockPlacePre;
import de.t14d3.rapunzellib.events.interact.UseBlockPre;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.rapunzellib.ZonesActionbar;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import de.t14d3.zones.rapunzellib.ZonesPermissionCache;
import de.t14d3.zones.utils.TypeKeys;
import de.t14d3.zones.utils.Types;

import java.util.concurrent.TimeUnit;

final class ZonesBlockListener {
    private final Zones zones;
    private final long bypassPermissionTtlNanos;

    private static final Flag[] BREAK = {Flags.BREAK};
    private static final Flag[] BREAK_CONTAINER = {Flags.BREAK, Flags.CONTAINER};
    private static final Flag[] BREAK_REDSTONE = {Flags.BREAK, Flags.REDSTONE};
    private static final Flag[] BREAK_CONTAINER_REDSTONE = {Flags.BREAK, Flags.CONTAINER, Flags.REDSTONE};

    private static final Flag[] PLACE = {Flags.PLACE};
    private static final Flag[] PLACE_CONTAINER = {Flags.PLACE, Flags.CONTAINER};
    private static final Flag[] PLACE_REDSTONE = {Flags.PLACE, Flags.REDSTONE};
    private static final Flag[] PLACE_CONTAINER_REDSTONE = {Flags.PLACE, Flags.CONTAINER, Flags.REDSTONE};

    private static final Flag[] INTERACT_CONTAINER = {Flags.INTERACT, Flags.CONTAINER};
    private static final Flag[] INTERACT_REDSTONE = {Flags.INTERACT, Flags.REDSTONE};
    private static final Flag[] INTERACT_TNT = {Flags.INTERACT, Flags.IGNITE};
    private static final Flag[] INTERACT_CONTAINER_REDSTONE = {Flags.INTERACT, Flags.CONTAINER, Flags.REDSTONE};
    private static final Flag[] INTERACT_CONTAINER_TNT = {Flags.INTERACT, Flags.CONTAINER, Flags.IGNITE};
    private static final Flag[] INTERACT_REDSTONE_TNT = {Flags.INTERACT, Flags.REDSTONE, Flags.IGNITE};
    private static final Flag[] INTERACT_CONTAINER_REDSTONE_TNT = {Flags.INTERACT, Flags.CONTAINER, Flags.REDSTONE, Flags.IGNITE};

    ZonesBlockListener(Zones zones) {
        this.zones = zones;
        int ttlSeconds = zones.getConfig().getInt("cache.permission-ttl", 2);
        this.bypassPermissionTtlNanos = TimeUnit.SECONDS.toNanos(Math.max(0, ttlSeconds));
    }

    GameEventBus.Subscription register(GameEventBus bus) {
        var s1 = bus.onPre(BlockBreakPre.class, event -> {
            RPlayer player = event.player();
            if (ZonesPermissionCache.hasPermissionCached(player, "zones.bypass.claimed", bypassPermissionTtlNanos))
                return;
            if (player.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false)) {
                event.deny();
                return;
            }

            String type = TypeKeys.normalize(event.block().typeKey());
            RWorldRef world = event.block().world().ref();
            RBlockPos loc = event.block().pos();

            boolean isContainer = Types.containers().contains(type);
            boolean isRedstone = Types.redstone().contains(type);
            Flag[] required = isContainer
                    ? (isRedstone ? BREAK_CONTAINER_REDSTONE : BREAK_CONTAINER)
                    : (isRedstone ? BREAK_REDSTONE : BREAK);

            if (!zones.getPermissionManager().checkActions(loc, world, player.uuid(), type, required)) {
                event.deny();
                ZonesActionbar.sendDenied(zones, player, loc, world, required, type);
            }
        });
        var s2 = bus.onPre(BlockPlacePre.class, event -> {
            RPlayer player = event.player();
            if (ZonesPermissionCache.hasPermissionCached(player, "zones.bypass.claimed", bypassPermissionTtlNanos))
                return;

            String type = TypeKeys.normalize(event.blockTypeKey());
            RWorldRef world = player.worldOrThrow().ref();
            RBlockPos loc = event.pos();

            boolean isContainer = Types.containers().contains(type);
            boolean isRedstone = Types.redstone().contains(type);
            Flag[] required = isContainer
                    ? (isRedstone ? PLACE_CONTAINER_REDSTONE : PLACE_CONTAINER)
                    : (isRedstone ? PLACE_REDSTONE : PLACE);

            if (!zones.getPermissionManager().checkActions(loc, world, player.uuid(), type, required)) {
                event.deny();
                ZonesActionbar.sendDenied(zones, player, loc, world, required, type);
            }
        });
        var s3 = bus.onPre(UseBlockPre.class, event -> {
            RPlayer player = event.player();
            if (ZonesPermissionCache.hasPermissionCached(player, "zones.bypass.claimed", bypassPermissionTtlNanos))
                return;

            String type = TypeKeys.normalize(event.block().typeKey());

            boolean isContainer = Types.containers().contains(type);
            boolean isRedstone = Types.redstone().contains(type);
            boolean isTnt = type.equals("tnt");
            if (!isContainer && !isRedstone && !isTnt) return;

            RWorldRef world = event.block().world().ref();
            RBlockPos loc = event.block().pos();

            Flag[] required = isContainer
                    ? (isRedstone
                    ? (isTnt ? INTERACT_CONTAINER_REDSTONE_TNT : INTERACT_CONTAINER_REDSTONE)
                    : (isTnt ? INTERACT_CONTAINER_TNT : INTERACT_CONTAINER))
                    : (isRedstone
                    ? (isTnt ? INTERACT_REDSTONE_TNT : INTERACT_REDSTONE)
                    : INTERACT_TNT);

            if (!zones.getPermissionManager().checkActions(loc, world, player.uuid(), type, required)) {
                event.deny();
                ZonesActionbar.sendDenied(zones, player, loc, world, required, type);
            }
        });
        return () -> {
            s1.close();
            s2.close();
            s3.close();
        };
    }
}
