package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.Direction;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class ExpandCommand {
    private final ZonesBukkit plugin;
    private RegionManager regionManager;
    private Messages messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ExpandCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand expand = new CommandAPICommand("expand")
            .withPermission("zones.expand")
            .withArguments(CustomArgument.region("key", "zones.expand.other", CustomArgument.MemberType.ADMIN),
                    new IntegerArgument("amount"),
                    new StringArgument("direction")
                            .setOptional(true)
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    List<String> directions = new ArrayList<>();
                                    for (Direction direction : Direction.values()) {
                                        directions.add(direction.name());
                                    }
                                    StringTooltip[] suggestions = new StringTooltip[directions.size()];
                                    int i = 0;
                                    for (String direction : directions) {
                                        suggestions[i++] = StringTooltip.ofMessage(direction,
                                                BukkitTooltip.messageFromAdventureComponent(
                                                        mm.deserialize(messages.get("commands.expand.direction"))));
                                    }
                                    return suggestions;
                                });
                            })),
                    new BooleanArgument("overlap")
                            .setOptional(true)
                            .withPermission("zones.expand.overlap")
            )
            .executes((sender, args) -> {
                        RegionKey regionKey = RegionKey.fromString(args.getRaw("key"));
                        Direction direction;
                        Region region = regionManager.regions().get(regionKey.getValue());
                        if (args.getRaw("direction") == null) {
                            if (sender instanceof Player player) {
                                direction = Direction.fromYaw(player.getLocation().getYaw());
                                if (region == null || !region.isAdmin(player.getUniqueId())) {
                                    sender.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                                    return;
                                }
                            } else {
                                sender.sendMessage(mm.deserialize(messages.get("commands.invalid")));
                                return;
                            }
                        } else {
                            direction = Direction.valueOf(args.getRaw("direction").toUpperCase());
                        }
                        int amount = Integer.parseInt(args.getRaw("amount"));
                        boolean allowOverlap = false;
                        if (args.getRaw("overlap") == null) {
                            allowOverlap = Objects.equals(args.getRaw("overlap"), "overlap") && sender.hasPermission(
                                    "zones.expand.overlap");
                        }
                        if (regionManager.expandBounds(region, direction, amount, allowOverlap)) {
                            sender.sendMessage(
                                    mm.deserialize(messages.get("commands.expand.success"),
                                            parsed("region", regionKey.toString())));
                        } else {
                            sender.sendMessage(
                                    mm.deserialize(messages.get("commands.expand.fail"),
                                            parsed("region", regionKey.toString())));
                        }
                    }
            );
}
