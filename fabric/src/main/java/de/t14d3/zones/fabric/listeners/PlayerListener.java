package de.t14d3.zones.fabric.listeners;

import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerListener {
    private final ZonesFabric mod;

    public PlayerListener(ZonesFabric mod) {
        this.mod = mod;

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (player.getMainHandStack().isEmpty()) {
                return ActionResult.PASS;
            }
            ItemStack itemStack = player.getMainHandStack();
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.PLACE);
            Block block = Block.getBlockFromItem(itemStack.getItem());
            if (block != Blocks.AIR) {
                if (block.getDefaultState().hasBlockEntity()) {
                    flags.add(Flags.CONTAINER);
                }
                if (block.getDefaultState().emitsRedstonePower()) {
                    flags.add(Flags.REDSTONE);
                }
            }

            ActionResult result = ActionResult.PASS;
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(hitResult.getBlockPos(), world, player, itemStack.getItem().getTranslationKey(),
                                flag)) {
                    result = ActionResult.FAIL;
                    break;
                }
            }
            return result;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            AtomicBoolean result = new AtomicBoolean(true);
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.PLACE);
            if (state.hasBlockEntity()) {
                flags.add(Flags.CONTAINER);
            }
            if (state.emitsRedstonePower()) {
                flags.add(Flags.REDSTONE);
            }
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(pos, world, player, state.getBlock().getTranslationKey(), flag)) {
                    result.set(false);
                    break;
                }
            }
            return result.get();
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (entity == null) {
                return ActionResult.PASS;
            }
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.INTERACT);
            flags.add(Flags.ENTITY);
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(entity.getBlockPos(), world, player, entity.getType().getTranslationKey(), flag)) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });
    }


}
