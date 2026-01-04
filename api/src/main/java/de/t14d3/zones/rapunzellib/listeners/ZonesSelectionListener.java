package de.t14d3.zones.rapunzellib.listeners;

import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import de.t14d3.zones.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;

final class ZonesSelectionListener {
    private final Zones zones;

    ZonesSelectionListener(Zones zones) {
        this.zones = zones;
    }

    GameEventBus.Subscription register(GameEventBus bus) {
        return bus.onPre(InteractBlockPre.class, event -> {
            RPlayer player = event.player();
            if (event.hand() == InteractBlockPre.Hand.OFF_HAND) return;

            boolean selecting = player.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false);
            if (!selecting) return;

            Box selection = player.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
            if (selection == null) {
                selection = new Box(null, null, event.block().world().ref(), false);
            }

            RWorldRef world = selection.getWorld() != null ? selection.getWorld() : event.block().world().ref();
            RBlockPos min = selection.getMin();
            RBlockPos max = selection.getMax();
            RBlockPos clicked = event.block().pos();

            event.deny();

            if (event.action() == InteractBlockPre.Action.LEFT_CLICK_BLOCK) {
                if (min != null) zones.getPlatform().removeBeacon(player, world, min);
                min = clicked;
                zones.getPlatform().showBeacon(player, min, world, NamedTextColor.GREEN);
                player.sendMessage(zones.getMessages().component(
                        "create.primary",
                        Placeholders.builder()
                                .string("x", String.valueOf(clicked.x()))
                                .string("y", String.valueOf(clicked.y()))
                                .string("z", String.valueOf(clicked.z()))
                                .build()
                ));
            } else {
                if (max != null) zones.getPlatform().removeBeacon(player, world, max);
                max = clicked;
                zones.getPlatform().showBeacon(player, max, world, NamedTextColor.RED);
                player.sendMessage(zones.getMessages().component(
                        "create.secondary",
                        Placeholders.builder()
                                .string("x", String.valueOf(clicked.x()))
                                .string("y", String.valueOf(clicked.y()))
                                .string("z", String.valueOf(clicked.z()))
                                .build()
                ));
            }

            Utils.SelectionMode mode = Utils.SelectionMode.getPlayerMode(player);
            if (mode != Utils.SelectionMode.CUBOID_3D) {
                if (min != null) min = new RBlockPos(min.x(), -63, min.z());
                if (max != null) max = new RBlockPos(max.x(), 319, max.z());
            }

            player.extras().put(ZonesExtraKeys.SELECTION, new Box(min, max, world, false));
        });
    }
}
