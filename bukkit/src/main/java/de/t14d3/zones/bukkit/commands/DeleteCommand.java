package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.commands.utils.CustomArgument;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

public class DeleteCommand {
    private RegionManager regionManager;
    private MessageFormatService messages;
    private final ZonesBukkit plugin;

    public DeleteCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand delete = new CommandAPICommand("delete")
            .withPermission("zones.delete")
            .withArguments(CustomArgument.region("key", "zones.delete.other", CustomArgument.MemberType.ADMIN))
            .executes((sender, args) -> {
                Region region = regionManager.regions().get(RegionKey.fromString(args.getRaw("key")).getValue());
                if (region == null) {
                    sender.sendMessage(messages.component("commands.invalid-region"));
                    return;
                }
                if (!sender.hasPermission("zones.delete.other")) {
                    if (sender instanceof Player player
                            && !regionManager.withWorldReadLock(region.getWorld(),
                            () -> region.isAdmin(player.getUniqueId()))) {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }
                }
                regionManager.deleteRegion(region.getKey());
                sender.sendMessage(
                        messages.component("commands.delete.success",
                                Placeholders.builder().string("region", region.getKey().toString()).build()));
                regionManager.triggerSave();
            });
}
