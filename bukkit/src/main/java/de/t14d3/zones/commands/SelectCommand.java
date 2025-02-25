package de.t14d3.zones.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.PlayerRepository;
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

public class SelectCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private ZonesBukkit plugin;

    public SelectCommand(ZonesBukkit plugin) {
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
                    de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUniqueId());
                    if (args.get("key") == null) {
                        region = regionManager.getEffectiveRegionAt(BlockLocation.of(player.getLocation()),
                                World.of(player.getWorld()));
                        if (region == null || region.getBounds().equals(zplayer.getSelection())) {
                            zplayer.setSelection(null);
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
                    if (zplayer.getSelection() == null || args.get("key") == null) {
                        zplayer.setSelection(region.getBounds());
                        player.sendMessage(mm.deserialize(messages.get("commands.select.selected"),
                                parsed("region", region.getName())));
                    } else {
                        zplayer.setSelection(null);
                        player.sendMessage(mm.deserialize(messages.get("commands.select.deselected")));
                    }
                }
            });
}
