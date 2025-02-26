package de.t14d3.zones.fabric.listeners;

import de.t14d3.zones.FabricPlatform;
import de.t14d3.zones.Region;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class PlayerListener {
    private final ZonesFabric mod;
    private final FabricPlatform platform;
    private final MiniMessage mm = MiniMessage.miniMessage();

    @SuppressWarnings("deprecation")
    public PlayerListener(ZonesFabric mod) {
        this.mod = mod;
        this.platform = mod.getPlatform();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            de.t14d3.zones.objects.Player zplayer = platform.getPlayer(player.getUuid());
            if (zplayer.getSelection() != null) {
                BlockLocation loc = new BlockLocation(
                        hitResult.getBlockPos().getX(),
                        hitResult.getBlockPos().getY(),
                        hitResult.getBlockPos().getZ()
                );
                if (hand.equals(Hand.MAIN_HAND)) {
                    zplayer.setSelection(
                            new Box(zplayer.getSelection().getMin(), loc, zplayer.getSelection().getWorld()));
                    zplayer.setSelectionCreating(true);
                    zplayer.sendMessage(mm.deserialize(mod.getMessages().get("create.primary"),
                            parsed("x", String.valueOf(loc.getX())),
                            parsed("y", String.valueOf(loc.getY())),
                            parsed("z", String.valueOf(loc.getZ()))
                    ));
                    return ActionResult.SUCCESS;
                }
            }
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
                        .checkAction(getOffset(hitResult), world, player, itemStack.getRegistryEntry().getIdAsString(),
                                flag)) {
                    result = ActionResult.FAIL;
                    break;
                }
            }
            if (result == ActionResult.FAIL) {
                sendActionBar(player, getOffset(hitResult), flags, itemStack.getRegistryEntry().getIdAsString());
            }
            return result;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            de.t14d3.zones.objects.Player zplayer = platform.getPlayer(player.getUuid());
            if (zplayer.getSelection() != null) {
                BlockLocation loc = new BlockLocation(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                );
                zplayer.setSelection(new Box(loc, zplayer.getSelection().getMax(), zplayer.getSelection().getWorld()));
                zplayer.setSelectionCreating(true);
                zplayer.sendMessage(mm.deserialize(mod.getMessages().get("create.secondary"),
                        parsed("x", String.valueOf(loc.getX())),
                        parsed("y", String.valueOf(loc.getY())),
                        parsed("z", String.valueOf(loc.getZ()))
                ));
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            AtomicBoolean result = new AtomicBoolean(true);
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.BREAK);
            if (state.hasBlockEntity()) {
                flags.add(Flags.CONTAINER);
            }
            if (state.emitsRedstonePower()) {
                flags.add(Flags.REDSTONE);
            }
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(pos, world, player, state.getBlock().getRegistryEntry().getIdAsString(), flag)) {
                    result.set(false);
                    break;
                }
            }
            if (!result.get()) {
                sendActionBar(player, pos, flags, state.getBlock().getRegistryEntry().getIdAsString());
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

    @NotNull
    private static BlockPos getOffset(BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Direction side = hitResult.getSide();
        if (side == Direction.DOWN) {
            return new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        } else if (side == Direction.UP) {
            return new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
        } else if (side == Direction.NORTH) {
            return new BlockPos(pos.getX(), pos.getY(), pos.getZ() - 1);
        } else if (side == Direction.SOUTH) {
            return new BlockPos(pos.getX(), pos.getY(), pos.getZ() + 1);
        } else if (side == Direction.WEST) {
            return new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ());
        } else if (side == Direction.EAST) {
            return new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ());
        }
        //noinspection DataFlowIssue
        return null;
    }

    private void sendActionBar(PlayerEntity player, BlockPos pos, List<Flag> requiredPermissions, String type) {
        List<Region> regions = mod.getRegionManager().getRegionsAt(BlockLocation.of(pos.getX(), pos.getY(), pos.getZ()),
                platform.getWorld(player.getWorld()));
        String regionNames = regions.stream().map(Region::getName).collect(Collectors.joining(", "));
        StringBuilder permissionsString = new StringBuilder();
        for (Flag action : requiredPermissions) {
            permissionsString.append(action.name()).append(", ");
        }
        if (!requiredPermissions.isEmpty()) {
            permissionsString.setLength(permissionsString.length() - 2); // Remove trailing ", "
        }
        de.t14d3.zones.objects.Player zplayer = platform.getPlayer(player.getUuid());
        zplayer.sendActionBar(mm.deserialize(mod.getMessages().get("region.no-interact-permission"),
                parsed("region", regionNames),
                parsed("actions", permissionsString.toString()),
                parsed("type", type))
        );

    }
}
