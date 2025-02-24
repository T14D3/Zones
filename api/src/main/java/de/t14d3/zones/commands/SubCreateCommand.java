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
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static de.t14d3.zones.visuals.BeaconUtils.resetBeacon;

public class SubCreateCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private Zones plugin;

    public SubCreateCommand(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand subcreate = new CommandAPICommand("subcreate")
            .withPermission("zones.subcreate")
            .withOptionalArguments(new StringArgument("key")
                    .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                        return CompletableFuture.supplyAsync(() -> {
                            List<Region> regions = new ArrayList<>();
                            if (info.sender().hasPermission("zones.subcreate.other")) {
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
                    if (!plugin.selection.containsKey(player.getUniqueId())) {
                        plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
                        player.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                        return; // Failure
                    }

                    Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
                    if (selectionPair.first() == null || selectionPair.second() == null) {
                        player.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                        return; // Failure
                    }

                    Region parentRegion = null;
                    if (args.get("key") == null) {
                        for (Region region : regionManager.getRegionsAt(player.getLocation())) {
                            if (region.isAdmin(player.getUniqueId())) {
                                parentRegion = region;
                                break;
                            }
                        }
                    } else {
                        RegionKey regionKey = RegionKey.fromString((String) args.get("key"));
                        Region tempRegion = regionManager.regions().get(regionKey.getValue());
                        if (tempRegion == null || !tempRegion.isAdmin(player.getUniqueId())) {
                            player.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                            return;
                        } else {
                            parentRegion = tempRegion;
                        }
                    }

                    if (parentRegion == null) {
                        player.sendMessage(mm.deserialize(messages.get("commands.subcreate.no-parent")));
                        return; // Failure
                    }

                    if (!parentRegion.contains(
                            selectionPair.first().toVector().toBlockVector()) || !parentRegion.contains(
                            selectionPair.second().toVector().toBlockVector())) {
                        player.sendMessage(mm.deserialize(messages.get("commands.subcreate.outside-parent")));
                        return; // Failure
                    }

                    Map<String, String> perms = new HashMap<>();
                    perms.put("role", "owner");

                    regionManager.createSubRegion(player.getName(), selectionPair.first().toVector().toBlockVector(),
                            selectionPair.second().toVector().toBlockVector(),
                            selectionPair.first().getWorld(), player.getUniqueId(), perms, parentRegion);
                    resetBeacon(player, selectionPair.first());
                    resetBeacon(player, selectionPair.second());
                    player.sendMessage(mm.deserialize(messages.get("commands.subcreate.success")));
                    plugin.selection.remove(player.getUniqueId());
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.only-player")));
                }
            });
}
