package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

import java.util.List;

public class InfoCommand {
    private RegionManager regionManager;
    private Messages messages;

    public InfoCommand(ZonesBukkit plugin) {
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand info = new CommandAPICommand("info")
            .withOptionalArguments(CustomArgument.region("key", "zones.info.other", CustomArgument.MemberType.MEMBER))
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
