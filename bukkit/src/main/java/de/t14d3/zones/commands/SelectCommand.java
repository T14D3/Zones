package de.t14d3.zones.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class SelectCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private Zones plugin;

    public SelectCommand(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand select = new CommandAPICommand("select")
            .withPermission("zones.select")
            .withArguments(
                    new StringArgument("key")
                            .setOptional(true)
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    List<Region> regions = new ArrayList<>();
                                    if (info.sender().hasPermission("zones.info.other")) {
                                        regions.addAll(regionManager.regions().values());
                                    } else if (info.sender() instanceof Player player) {
                                        for (Region region : regionManager.regions().values()) {
                                            if (region.isMember(player.getUniqueId())) {
                                                regions.add(region);
                                            }
                                        }
                                    }
                                    StringTooltip[] suggestions = new StringTooltip[regions.size()];
                                    int i = 0;
                                    for (Region region : regions) {
                                        suggestions[i++] = StringTooltip.ofMessage(region.getKey().toString(),
                                                BukkitTooltip.messageFromAdventureComponent(
                                                        Messages.regionInfo(region, false)));
                                    }
                                    return suggestions;
                                });
                            })))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    Region region;
                    if (args.get("key") == null) {
                        region = regionManager.getEffectiveRegionAt(player.getLocation());
                        if (region == null) {
                            plugin.particles.remove(player.getUniqueId());
                            player.sendMessage(mm.deserialize(messages.get("commands.select.deselected")));
                            return;
                        }
                    } else {
                        region = regionManager.regions().get(RegionKey.fromString(args.getRaw("key")).getValue());
                    }
                    if (region == null) {
                        player.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                        return;
                    }
                    if (!plugin.particles.containsKey(player.getUniqueId()) || args.get("key") == null) {
                        plugin.particles.put(player.getUniqueId(), BoundingBox.of(region.getMin(), region.getMax()));
                        player.sendMessage(mm.deserialize(messages.get("commands.select.selected"),
                                parsed("region", region.getName())));
                    } else {
                        plugin.particles.remove(player.getUniqueId());
                        player.sendMessage(mm.deserialize(messages.get("commands.select.deselected")));
                    }
                }
            });
}
