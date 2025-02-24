package de.t14d3.zones.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InfoCommand {
    private RegionManager regionManager;
    private Messages messages;

    public InfoCommand(ZonesBukkit plugin) {
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand info = new CommandAPICommand("info")
            .withOptionalArguments(new StringArgument("key")
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
                List<Region> regions;
                Player player = null;
                if (sender instanceof Player temp) {
                    player = temp;
                }
                if (args.get("key") == null) {
                    if (player != null) {
                        regions = regionManager.getRegionsAt(BlockLocation.of(player.getLocation()),
                                World.of(player.getWorld()));
                    } else {
                        sender.sendMessage(messages.getCmp("commands.invalid-region"));
                        return;
                    }
                } else {
                    try {
                        regions = List.of(RegionManager.getRegion(RegionKey.fromString((String) args.get("key"))));
                    } catch (Exception e) {
                        sender.sendMessage(messages.getCmp("commands.invalid-region"));
                        return;
                    }
                }

                if (regions.isEmpty()) {
                    sender.sendMessage(messages.getCmp("commands.invalid-region"));
                    return;
                }
                for (Region region : regions) {
                    if (sender.hasPermission("zones.info.other")) {
                        sender.sendMessage(Messages.regionInfo(region, true));
                    } else if (player != null && region.isMember(player.getUniqueId())) {
                        sender.sendMessage(Messages.regionInfo(region, region.isAdmin(player.getUniqueId())));
                    }
                }
            });
}
