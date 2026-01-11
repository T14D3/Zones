package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.commands.utils.CustomArgument;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

import java.util.List;

public class InfoCommand {
    private RegionManager regionManager;
    private MessageFormatService messages;

    public InfoCommand(ZonesBukkit plugin) {
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand info = new CommandAPICommand("info")
            .withOptionalArguments(CustomArgument.region("key", "zones.info.other", CustomArgument.MemberType.MEMBER))
            .executes((sender, args) -> {
                List<Region> regions;
                Player player = sender instanceof Player ? (Player) sender : null;
                Region region = (Region) args.get("key");
                if (region == null) {
                    if (player != null) {
                        RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
                        if (rPlayer == null) return;
                        RBlockPos pos = rPlayer.locationOrThrow().blockPos();
                        RWorldRef world = rPlayer.worldOrThrow().ref();
                        regions = regionManager.getRegionsAt(pos, world);
                        if (regions.isEmpty()) {
                            sender.sendMessage(messages.component("region.none-found"));
                            return;
                        }
                    } else {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }
                } else {
                    regions = List.of(region);
                }
                for (Region region_ : regions) {
                    if (sender.hasPermission("zones.info.other")) {
                        sender.sendMessage(regionManager.withWorldReadLock(region_.getWorld(),
                                () -> Messages.regionInfo(region_, true)));
                    } else if (player != null) {
                        var msg = regionManager.withWorldReadLock(region_.getWorld(), () -> {
                            if (!region_.isMember(player.getUniqueId())) return null;
                            return Messages.regionInfo(region_, region_.isAdmin(player.getUniqueId()));
                        });
                        if (msg != null) sender.sendMessage(msg);
                    }
                }
            });
}
