package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class DeleteCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
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
