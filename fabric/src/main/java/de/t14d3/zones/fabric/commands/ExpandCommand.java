package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Direction;
import de.t14d3.zones.visuals.particles.ParticlePalette;
import de.t14d3.zones.visuals.particles.ParticleRenderMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExpandCommand {
    private static final String PREVIEW_ID = "expand-preview";
    private static final long PREVIEW_THROTTLE_NANOS = Duration.ofMillis(100).toNanos();
    private static final Duration PREVIEW_TTL = Duration.ofSeconds(10);

    private enum PreviewStage {
        AMOUNT,
        DIRECTION
    }

    private final ZonesFabric mod;
    private final RegionManager regionManager;
    private final MessageFormatService messages;
    private final ConcurrentHashMap<UUID, Long> lastPreviewNanos = new ConcurrentHashMap<>();

    public ExpandCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        ServerPlayer nativePlayer = context.getSource().getPlayer();
        if (nativePlayer != null) {
            mod.getZones().getParticleVisualManager().removeOverlay(nativePlayer.getUUID(), PREVIEW_ID);
        }

        RegionKey key;
        try {
            key = RegionKey.fromString(context.getArgument("key", String.class));
        } catch (Exception ignored) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }

        Region region = regionManager.regions().get(key.getValue());
        if (region == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }
        if (!CommandPermissions.check(context.getSource(), "zones.expand.other")) {
            if (context.getSource().getPlayer() == null
                    || !regionManager.withWorldReadLock(region.getWorld(),
                    () -> region.isOwner(context.getSource().getPlayer().getUUID()))) {
                context.getSource().sendMessage(messages.component("commands.invalid-region"));
                return 1;
            }
        }
        int amount = context.getArgument("amount", Integer.class);
        boolean allowOverlap = false;
        try {
            allowOverlap = context.getArgument("overlap", Boolean.class) && CommandPermissions.check(
                    context.getSource(),
                    "zones.expand.overlap");
        } catch (Exception ignored) {
        }

        Direction direction;
        try {
            direction = Direction.valueOf(context.getArgument("direction", String.class).toUpperCase());
        } catch (Exception ignored) {
            if (context.getSource().getPlayer() != null) {
                direction = Direction.fromYaw(context.getSource().getPlayer().getViewYRot(1.0f));
            } else {
                context.getSource().sendMessage(messages.component("commands.invalid"));
                return 1;
            }
        }
        Box preview = computeExpandedPreview(region, direction, amount);
        if (preview == null || preview.getMin() == null || preview.getMax() == null) {
            context.getSource().sendMessage(messages.component("commands.expand.fail",
                    Placeholders.builder().string("region", region.getKey().toString()).build()));
            return 1;
        }

        Region parent = region.getParentRegion(regionManager);
        if (parent != null) {
            boolean insideParent = regionManager.withWorldReadLock(parent.getWorld(),
                    () -> parent.contains(preview.getMin()) && parent.contains(preview.getMax()));
            if (!insideParent) {
                context.getSource().sendMessage(messages.component("commands.expand.outside-parent"));
                return 1;
            }
        }

        if (!allowOverlap && regionManager.overlapsOutsideLineage(region, preview.getMin(), preview.getMax())) {
            context.getSource().sendMessage(messages.component("commands.create.overlap"));
            return 1;
        }
        if (regionManager.expandBounds(region, direction, amount, allowOverlap)) {
            context.getSource().sendMessage(messages.component("commands.expand.success",
                    Placeholders.builder().string("region", region.getKey().toString()).build()));
        } else {
            context.getSource().sendMessage(messages.component("commands.expand.fail",
                    Placeholders.builder().string("region", region.getKey().toString()).build()));
        }
        return 1;
    }

    private void maybePreview(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, PreviewStage stage) {
        if (context == null || builder == null || stage == null) return;
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return;

        long now = System.nanoTime();
        Long last = lastPreviewNanos.put(player.getUUID(), now);
        if (last != null && (now - last) < PREVIEW_THROTTLE_NANOS) return;

        String rawKey;
        try {
            rawKey = context.getArgument("key", String.class);
        } catch (Exception ignored) {
            mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
            return;
        }

        if (rawKey == null || rawKey.isBlank()) {
            mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
            return;
        }
        rawKey = rawKey.trim();
        if (rawKey.length() >= 2 && rawKey.startsWith("\"") && rawKey.endsWith("\"")) {
            rawKey = rawKey.substring(1, rawKey.length() - 1).trim();
        }

        Integer parsedAmount;
        if (stage == PreviewStage.DIRECTION) {
            try {
                parsedAmount = context.getArgument("amount", Integer.class);
            } catch (Exception ignored) {
                parsedAmount = null;
            }
        } else {
            parsedAmount = tryParseInt(builder.getRemaining());
        }

        if (parsedAmount == null || parsedAmount == 0) {
            mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
            return;
        }
        final int amount = parsedAmount;

        String rawDirection = stage == PreviewStage.DIRECTION ? builder.getRemaining() : null;
        Direction direction = resolveDirection(rawDirection, player);

        RegionKey key;
        try {
            key = RegionKey.fromString(rawKey);
        } catch (Exception ignored) {
            mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
            return;
        }

        Region region = regionManager.regions().get(key.getValue());
        if (region == null) {
            mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
            return;
        }

        boolean expandOther = CommandPermissions.check(context.getSource(), "zones.expand.other");
        regionManager.withWorldReadLock(region.getWorld(), () -> {
            if (!expandOther && !region.isOwner(player.getUUID())) {
                mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
                return;
            }

            Box preview = computeExpandedPreview(region, direction, amount);
            if (preview == null || preview.getMin() == null || preview.getMax() == null) {
                mod.getZones().getParticleVisualManager().removeOverlay(player.getUUID(), PREVIEW_ID);
                return;
            }

            mod.getZones().getParticleVisualManager().showOverlay(
                    player.getUUID(),
                    PREVIEW_ID,
                    preview,
                    ParticleRenderMode.WIRE_EDGES,
                    ParticlePalette.PREVIEW,
                    50,
                    1,
                    PREVIEW_TTL
            );
        });
    }

    private static Direction resolveDirection(String rawDirection, ServerPlayer player) {
        if (player == null) return Direction.NORTH;
        if (rawDirection == null || rawDirection.isBlank()) {
            return Direction.fromYaw(player.getViewYRot(1.0f));
        }

        String prefix = rawDirection.trim();
        if (prefix.length() >= 2 && prefix.startsWith("\"") && !prefix.endsWith("\"")) {
            prefix = prefix.substring(1);
        }
        if (prefix.length() >= 2 && prefix.startsWith("\"") && prefix.endsWith("\"")) {
            prefix = prefix.substring(1, prefix.length() - 1);
        }
        prefix = prefix.trim().toUpperCase();

        Direction match = null;
        for (Direction d : Direction.values()) {
            if (d.name().equals(prefix)) return d;
            if (d.name().startsWith(prefix)) {
                if (match != null) return Direction.fromYaw(player.getViewYRot(1.0f));
                match = d;
            }
        }
        return match != null ? match : Direction.fromYaw(player.getViewYRot(1.0f));
    }

    private static Integer tryParseInt(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        int end = 0;
        boolean signed = s.charAt(0) == '-' || s.charAt(0) == '+';
        if (signed) end = 1;
        while (end < s.length() && Character.isDigit(s.charAt(end))) end++;
        if (end == 0 || (end == 1 && signed)) return null;

        try {
            return Integer.parseInt(s.substring(0, end));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Box computeExpandedPreview(Region region, Direction direction, int amount) {
        if (region == null || direction == null) return null;
        var min = region.getMin();
        var max = region.getMax();
        var world = region.getWorld();
        if (min == null || max == null || world == null) return null;

        RBlockPos newMin = min;
        RBlockPos newMax = max;

        switch (direction) {
            case NORTH -> newMin = new RBlockPos(newMin.x(), newMin.y(), newMin.z() - amount);
            case SOUTH -> newMax = new RBlockPos(newMax.x(), newMax.y(), newMax.z() + amount);
            case WEST -> newMin = new RBlockPos(newMin.x() - amount, newMin.y(), newMin.z());
            case EAST -> newMax = new RBlockPos(newMax.x() + amount, newMax.y(), newMax.z());
            case DOWN -> newMin = new RBlockPos(newMin.x(), newMin.y() - amount, newMin.z());
            case UP -> newMax = new RBlockPos(newMax.x(), newMax.y() + amount, newMax.z());
        }

        if (newMin.x() > newMax.x() || newMin.y() > newMax.y() || newMin.z() > newMax.z()) {
            return null;
        }

        return new Box(newMin, newMax, world, true);
    }

    LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("expand")
                .requires(source -> CommandPermissions.check(source, "zones.expand"))
                .then(Commands.argument("key", StringArgumentType.string())     
                        .suggests(RootCommand::regionKeySuggestion)
                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                .suggests((context, builder) -> {
                                    maybePreview(context, builder, PreviewStage.AMOUNT);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("direction", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            maybePreview(context, builder, PreviewStage.DIRECTION);
                                            builder.suggest("north");
                                            builder.suggest("east");
                                            builder.suggest("south");
                                            builder.suggest("west");
                                            builder.suggest("up");
                                            builder.suggest("down");
                                            return builder.buildFuture();
                                        })
                                        .executes(this::execute)
                                        .then(Commands.argument("overlap", BoolArgumentType.bool())
                                                .requires(source -> CommandPermissions.check(source,
                                                        "zones.expand.overlap"))
                                                .executes(this::execute)))));
    }
}
