package de.t14d3.zones.fabric.listeners;

import de.t14d3.zones.Region;
import de.t14d3.zones.fabric.FabricPlatform;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
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

    public PlayerListener(ZonesFabric mod) {
        this.mod = mod;
        this.platform = mod.getPlatform();

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            de.t14d3.zones.objects.Player zplayer = platform.getPlayer(player.getUUID());
            if (zplayer.getSelection() != null && zplayer.isSelectionCreating()) {
                if (hand.equals(InteractionHand.MAIN_HAND)) {
                    BlockLocation min = zplayer.getSelection().getMin();
                    BlockLocation loc = new BlockLocation(
                            hitResult.getBlockPos().getX(),
                            hitResult.getBlockPos().getY(),
                            hitResult.getBlockPos().getZ()
                    );
                    platform.removeBeacon(zplayer, zplayer.getSelection().getWorld(), min);
                    min = loc;
                    platform.showBeacon(zplayer, min, zplayer.getSelection().getWorld(), NamedTextColor.GREEN);
                    zplayer.setSelection(
                            new Box(min, zplayer.getSelection().getMax(), zplayer.getSelection().getWorld(), false));
                    zplayer.setSelectionCreating(true);
                    zplayer.sendMessage(mm.deserialize(mod.getMessages().get("create.primary"),
                            parsed("x", String.valueOf(loc.getX())),
                            parsed("y", String.valueOf(loc.getY())),
                            parsed("z", String.valueOf(loc.getZ()))
                    ));
                    return InteractionResult.FAIL;
                }
                return InteractionResult.PASS;
            }
            if (player.getMainHandItem().isEmpty()) {
                return InteractionResult.PASS;
            }
            ItemStack itemStack = player.getMainHandItem();
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.PLACE);
            Block block = Block.byItem(itemStack.getItem());
            if (block != Blocks.AIR) {
                if (block.defaultBlockState().hasBlockEntity()) {
                    flags.add(Flags.CONTAINER);
                }
                if (block.defaultBlockState().isSignalSource()) {
                    flags.add(Flags.REDSTONE);
                }
            }

            InteractionResult result = InteractionResult.PASS;
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(getOffset(hitResult), world, player, itemStack.getItem().getDescriptionId(),
                                flag)) {
                    result = InteractionResult.FAIL;
                    break;
                }
            }
            if (result == InteractionResult.FAIL) {
                sendActionBar(player, getOffset(hitResult), flags, itemStack.getItem().getDescriptionId());
            }
            return result;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            de.t14d3.zones.objects.Player zplayer = platform.getPlayer(player.getUUID());
            if (zplayer.getSelection() != null && zplayer.isSelectionCreating()) {
                BlockLocation max = zplayer.getSelection().getMax();
                BlockLocation loc = new BlockLocation(
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                );
                platform.removeBeacon(zplayer, zplayer.getSelection().getWorld(), max);
                max = loc;
                platform.showBeacon(zplayer, max, zplayer.getSelection().getWorld(), NamedTextColor.RED);
                zplayer.setSelection(
                        new Box(zplayer.getSelection().getMin(), max, zplayer.getSelection().getWorld(), false));
                zplayer.setSelectionCreating(true);
                zplayer.sendMessage(mm.deserialize(mod.getMessages().get("create.secondary"),
                        parsed("x", String.valueOf(loc.getX())),
                        parsed("y", String.valueOf(loc.getY())),
                        parsed("z", String.valueOf(loc.getZ()))
                ));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            AtomicBoolean result = new AtomicBoolean(true);
            List<Flag> flags = new ArrayList<>();
            flags.add(Flags.BREAK);
            if (state.hasBlockEntity()) {
                flags.add(Flags.CONTAINER);
            }
            if (state.isSignalSource()) {
                flags.add(Flags.REDSTONE);
            }
            for (Flag flag : flags) {
                if (!mod.getPermissionManager()
                        .checkAction(pos, world, player, state.getBlock().getDescriptionId(), flag)) {
                    result.set(false);
                    break;
                }
            }
            if (!result.get()) {
                sendActionBar(player, pos, flags, state.getBlock().getDescriptionId());
            }

            return result.get();
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
    }

    @NotNull
    private static BlockPos getOffset(BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        Direction side = hitResult.getDirection();
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

    private void sendActionBar(Player player, BlockPos pos, List<Flag> requiredPermissions, String type) {
        List<Region> regions = mod.getRegionManager().getRegionsAt(BlockLocation.of(pos.getX(), pos.getY(), pos.getZ()),
                platform.getWorld(player.getCommandSenderWorld()));
        String regionNames = regions.stream().map(Region::getName).collect(Collectors.joining(", "));
        StringBuilder permissionsString = new StringBuilder();
        for (Flag action : requiredPermissions) {
            permissionsString.append(action.name()).append(", ");
        }
        if (!requiredPermissions.isEmpty()) {
            permissionsString.setLength(permissionsString.length() - 2); // Remove trailing ", "
        }
        de.t14d3.zones.objects.Player zplayer = platform.getPlayer(player.getUUID());
        zplayer.sendActionBar(mm.deserialize(mod.getMessages().get("region.no-interact-permission"),
                parsed("region", regionNames),
                parsed("actions", permissionsString.toString()),
                parsed("type", type))
        );

    }
}
