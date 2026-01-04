package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.commands.utils.CustomArgument;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class RenameCommand {
    private RegionManager regionManager;
    private MessageFormatService messages;
    private final ZonesBukkit plugin;

    public RenameCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand rename = new CommandAPICommand("rename")
            .withPermission("zones.rename")
            .withArguments(CustomArgument.region("key", "zones.rename.other", CustomArgument.MemberType.ADMIN),
                    new StringArgument("New Name"))
            .executes((sender, args) -> {
                Region region = regionManager.regions().get(RegionKey.fromString(args.getRaw("key")).getValue());
                if (region == null) {
                    sender.sendMessage(messages.component("commands.invalid-region"));
                    return;
                }
                if (!sender.hasPermission("zones.rename.other")) {
                    if (sender instanceof Player player
                            && !regionManager.withWorldReadLock(region.getWorld(),
                            () -> region.isAdmin(player.getUniqueId()))) {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }
                }
                String name = args.getRaw("New Name");
                regionManager.withWorldWriteLock(region.getWorld(), () -> region.setName(name, regionManager));
                sender.sendMessage(messages.component(
                        "commands.rename.success",
                        Placeholders.builder()
                                .string("region", region.getKey().toString())
                                .string("name", name)
                                .build()
                ));
            });
}
