package de.t14d3.zones.fabric.listeners;

import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.fabric.FabricPlatform;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener {
    private final ZonesFabric mod;
    private final FabricPlatform platform;

    public PlayerListener(ZonesFabric mod) {
        this.mod = mod;
        this.platform = mod.getPlatform();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
            if (rPlayer == null) return InteractionResult.PASS;

            boolean selecting = rPlayer.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false);
            Box selection = rPlayer.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
            if (selection != null && selecting) {
                if (hand.equals(InteractionHand.MAIN_HAND)) {
                    RBlockPos min = selection.getMin();
                    RBlockPos max = selection.getMax();
                    RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : rPlayer.worldOrThrow()
                            .ref();

                    RBlockPos loc = new RBlockPos(
                            hitResult.getBlockPos().getX(),
                            hitResult.getBlockPos().getY(),
                            hitResult.getBlockPos().getZ()
                    );
                    platform.removeBeacon(rPlayer, selectionWorld, min);
                    min = loc;
                    platform.showBeacon(rPlayer, min, selectionWorld, NamedTextColor.GREEN);
                    rPlayer.extras().put(ZonesExtraKeys.SELECTION, new Box(min, max, selectionWorld, false));
                    rPlayer.sendMessage(mod.getMessages().component("create.primary",
                            Placeholders.builder()
                                    .string("x", String.valueOf(loc.x()))
                                    .string("y", String.valueOf(loc.y()))
                                    .string("z", String.valueOf(loc.z()))
                                    .build()));
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            return InteractionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
            if (rPlayer == null) return InteractionResult.PASS;

            boolean selecting = rPlayer.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false);
            Box selection = rPlayer.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
            if (selection != null && selecting) {
                RBlockPos min = selection.getMin();
                RBlockPos max = selection.getMax();
                RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : rPlayer.worldOrThrow()
                        .ref();

                RBlockPos loc = new RBlockPos(pos.getX(), pos.getY(), pos.getZ());
                platform.removeBeacon(rPlayer, selectionWorld, max);
                max = loc;
                platform.showBeacon(rPlayer, max, selectionWorld, NamedTextColor.RED);
                rPlayer.extras().put(ZonesExtraKeys.SELECTION, new Box(min, max, selectionWorld, false));
                rPlayer.sendMessage(mod.getMessages().component("create.secondary",
                        Placeholders.builder()
                                .string("x", String.valueOf(loc.x()))
                                .string("y", String.valueOf(loc.y()))
                                .string("z", String.valueOf(loc.z()))
                                .build()));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity == null) {
                return InteractionResult.PASS;
            }
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.INTERACT);
            flags.add(Flags.ENTITY);
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(entity.getOnPos(), world, player, entity.getType().getDescriptionId(), flag)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity == null) {
                return InteractionResult.PASS;
            }
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.DAMAGE);
            flags.add(Flags.ENTITY);
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(entity.getOnPos(), world, player, entity.getType().getDescriptionId(), flag)) {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });
    }
}
