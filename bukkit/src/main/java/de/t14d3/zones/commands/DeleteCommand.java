package de.t14d3.zones.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesBukkit;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class DeleteCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private ZonesBukkit plugin;

    public DeleteCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand delete = new CommandAPICommand("delete")
            .withPermission("zones.delete")
            .withArguments(
                    new StringArgument("key")
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    List<Region> regions = new ArrayList<>();
                                    if (info.sender().hasPermission("zones.delete.other")) {
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
                Region region = regionManager.regions().get(RegionKey.fromString(args.getRaw("key")).getValue());
                if (region == null) {
                    sender.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                    return;
                }
                if (!sender.hasPermission("zones.delete.other")) {
                    if (sender instanceof Player player && !region.isAdmin(player.getUniqueId())) {
                        sender.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                        return;
                    }
                }
                regionManager.deleteRegion(region.getKey());
                sender.sendMessage(
                        mm.deserialize(messages.get("commands.delete.success"),
                                parsed("region", region.getKey().toString())));
                regionManager.triggerSave();
            });
}
