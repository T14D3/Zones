package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.commands.utils.CustomArgument;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Direction;
import de.t14d3.zones.visuals.particles.ParticlePalette;
import de.t14d3.zones.visuals.particles.ParticleRenderMode;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ExpandCommand {
    private static final String PREVIEW_ID = "expand-preview";
    private static final long PREVIEW_THROTTLE_NANOS = Duration.ofMillis(100).toNanos();
    private static final Duration PREVIEW_TTL = Duration.ofSeconds(10);

    private final ZonesBukkit plugin;
    private final RegionManager regionManager;
    private final MessageFormatService messages;
    private final ConcurrentHashMap<UUID, Long> lastPreviewNanos = new ConcurrentHashMap<>();
    public final CommandAPICommand expand;

    public ExpandCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();

        this.expand = new CommandAPICommand("expand")
                .withPermission("zones.expand")
                .withArguments(CustomArgument.region("key", "zones.expand.other", CustomArgument.MemberType.ADMIN),
                        new IntegerArgument("amount")
                                .replaceSuggestions(ArgumentSuggestions.stringsAsync(info -> {
                                    maybePreview(info, PreviewStage.AMOUNT);
                                    return CompletableFuture.completedFuture(new String[0]);
                                })),
                        new StringArgument("direction")
                                .setOptional(true)
                                .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                    return CompletableFuture.supplyAsync(() -> {
                                        maybePreview(info, PreviewStage.DIRECTION);
                                        List<String> directions = new ArrayList<>();
                                        for (Direction direction : Direction.values()) {
                                            directions.add(direction.name());
                                        }
                                        StringTooltip[] suggestions = new StringTooltip[directions.size()];
                                        int i = 0;
                                        for (String direction : directions) {
                                            suggestions[i++] = StringTooltip.ofMessage(direction,
                                                    BukkitTooltip.messageFromAdventureComponent(
                                                            messages.component("commands.expand.direction")));
                                        }
                                        return suggestions;
                                    });
                                })),
                        new BooleanArgument("overlap")
                                .setOptional(true)
                                .withPermission("zones.expand.overlap")
                )
                .executes((sender, args) -> {
                            if (sender instanceof Player player) {
                                plugin.getZones().getParticleVisualManager().removeOverlay(player.getUniqueId(), PREVIEW_ID);
                            }
                            RegionKey regionKey = RegionKey.fromString(args.getRaw("key"));
                            Direction direction;
                            Region region = regionManager.regions().get(regionKey.getValue());
                            String directionArg = args.getRaw("direction");
                            if (directionArg == null || directionArg.isBlank()) {
                                if (sender instanceof Player player) {
                                    direction = Direction.fromYaw(player.getLocation().getYaw());
                                    if (region == null
                                            || !regionManager.withWorldReadLock(region.getWorld(),
                                            () -> region.isAdmin(player.getUniqueId()))) {
                                        sender.sendMessage(messages.component("commands.invalid-region"));
                                        return;
                                    }
                                } else {
                                    sender.sendMessage(messages.component("commands.invalid"));
                                    return;
                                }
                            } else {
                                direction = Direction.valueOf(directionArg.toUpperCase());
                            }
                            int amount = args.getByClassOrDefault("amount", Integer.class, 0);
                            boolean overlapArg = args.getByClassOrDefault("overlap", Boolean.class, false);
                            boolean allowOverlap = overlapArg && sender.hasPermission("zones.expand.overlap");
                    Box preview = computeExpandedPreview(region, direction, amount);
                    if (preview == null || preview.getMin() == null || preview.getMax() == null) {
                        sender.sendMessage(messages.component("commands.expand.fail",
                                Placeholders.builder().string("region", regionKey.toString()).build()));
                        return;
                    }

                    Region parent = region.getParentRegion(regionManager);
                    if (parent != null) {
                        boolean insideParent = regionManager.withWorldReadLock(parent.getWorld(),
                                () -> parent.contains(preview.getMin()) && parent.contains(preview.getMax()));
                        if (!insideParent) {
                            sender.sendMessage(messages.component("commands.expand.outside-parent"));
                            return;
                        }
                    }

                    if (!allowOverlap && regionManager.overlapsOutsideLineage(region, preview.getMin(),
                            preview.getMax())) {
                        sender.sendMessage(messages.component("commands.create.overlap"));
                        return;
                    }

                            if (regionManager.expandBounds(region, direction, amount, allowOverlap)) {
                                sender.sendMessage(
                                        messages.component("commands.expand.success",
                                                Placeholders.builder().string("region", regionKey.toString()).build()));
                            } else {
                                sender.sendMessage(
                                        messages.component("commands.expand.fail",
                                                Placeholders.builder().string("region", regionKey.toString()).build()));
                            }
                        }
                );
    }

    private enum PreviewStage {
        AMOUNT,
        DIRECTION
    }

    private void maybePreview(SuggestionInfo<CommandSender> info, PreviewStage stage) {
        if (info == null || stage == null) return;
        if (!(info.sender() instanceof Player player)) return;

        long now = System.nanoTime();
        Long last = lastPreviewNanos.put(player.getUniqueId(), now);
        if (last != null && (now - last) < PREVIEW_THROTTLE_NANOS) return;

        CommandArguments prev = info.previousArgs();
        if (prev == null) return;

        String rawKey = prev.getRaw("key");
        if (rawKey == null || rawKey.isBlank()) {
            plugin.getZones().getParticleVisualManager().removeOverlay(player.getUniqueId(), PREVIEW_ID);
            return;
        }

        String rawAmount = stage == PreviewStage.AMOUNT ? info.currentArg() : prev.getRaw("amount");
        Integer amount = tryParseInt(rawAmount);
        if (amount == null || amount == 0) {
            plugin.getZones().getParticleVisualManager().removeOverlay(player.getUniqueId(), PREVIEW_ID);
            return;
        }

        String rawDirection = stage == PreviewStage.DIRECTION ? info.currentArg() : null;
        Direction direction = resolveDirection(rawDirection, player);

        RegionKey key = RegionKey.fromString(rawKey);
        Region region = regionManager.regions().get(key.getValue());
        if (region == null) {
            plugin.getZones().getParticleVisualManager().removeOverlay(player.getUniqueId(), PREVIEW_ID);
            return;
        }

        regionManager.withWorldReadLock(region.getWorld(), () -> {
            var min = region.getMin();
            var max = region.getMax();
            if (min == null || max == null) {
                plugin.getZones().getParticleVisualManager().removeOverlay(player.getUniqueId(), PREVIEW_ID);
                return;
            }

            Box preview = computeExpandedPreview(region, direction, amount);
            if (preview == null || preview.getMin() == null || preview.getMax() == null) {
                plugin.getZones().getParticleVisualManager().removeOverlay(player.getUniqueId(), PREVIEW_ID);
                return;
            }

            plugin.getZones().getParticleVisualManager().showOverlay(
                    player.getUniqueId(),
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

    private static Direction resolveDirection(String rawDirection, Player player) {
        if (player == null) return Direction.NORTH;
        if (rawDirection == null || rawDirection.isBlank()) {
            return Direction.fromYaw(player.getLocation().getYaw());
        }

        String prefix = rawDirection.trim().toUpperCase();
        Direction exact = null;
        for (Direction d : Direction.values()) {
            if (d.name().equals(prefix)) return d;
            if (d.name().startsWith(prefix)) {
                if (exact != null) return Direction.fromYaw(player.getLocation().getYaw());
                exact = d;
            }
        }
        return exact != null ? exact : Direction.fromYaw(player.getLocation().getYaw());
    }

    private static Integer tryParseInt(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        int end = 0;
        boolean signed = s.charAt(0) == '-' || s.charAt(0) == '+';
        if (signed) end = 1;
        while (end < s.length() && Character.isDigit(s.charAt(end))) end++;
        if (end == 0 || (end == 1 && (signed))) return null;

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

        var newMin = min;
        var newMax = max;

        switch (direction) {
            case NORTH ->
                    newMin = new de.t14d3.rapunzellib.objects.RBlockPos(newMin.x(), newMin.y(), newMin.z() - amount);
            case SOUTH ->
                    newMax = new de.t14d3.rapunzellib.objects.RBlockPos(newMax.x(), newMax.y(), newMax.z() + amount);
            case WEST ->
                    newMin = new de.t14d3.rapunzellib.objects.RBlockPos(newMin.x() - amount, newMin.y(), newMin.z());
            case EAST ->
                    newMax = new de.t14d3.rapunzellib.objects.RBlockPos(newMax.x() + amount, newMax.y(), newMax.z());
            case DOWN ->
                    newMin = new de.t14d3.rapunzellib.objects.RBlockPos(newMin.x(), newMin.y() - amount, newMin.z());
            case UP -> newMax = new de.t14d3.rapunzellib.objects.RBlockPos(newMax.x(), newMax.y() + amount, newMax.z());
        }

        if (newMin.x() > newMax.x() || newMin.y() > newMax.y() || newMin.z() > newMax.z()) {
            return null;
        }

        return new Box(newMin, newMax, world, true);
    }
}
