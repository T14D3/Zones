package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

import java.util.List;

public class ListCommand {
    private RegionManager regionManager;
    private MessageFormatService messages;

    public ListCommand(ZonesBukkit plugin) {
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand list = new CommandAPICommand("list")
            .withPermission("zones.list")
            .withOptionalArguments(new IntegerArgument("page"))
            .executes((sender, args) -> {
                boolean perm = sender.hasPermission("zones.info.other");
                int page = args.get("page") == null ? 1 : (int) args.get("page");
                if (page < 1) {
                    page = 1;
                }

                Player player = sender instanceof Player temp ? temp : null;
                var uuid = player != null ? player.getUniqueId() : null;
                List<Region> regions = regionManager.regions().values().stream()
                        .filter(region -> perm || (uuid != null && regionManager.withWorldReadLock(region.getWorld(),
                                () -> region.isMember(uuid))))
                        .toList();
                if (regions.isEmpty()) {
                    sender.sendMessage(messages.component("region.none-found"));
                    return;
                }
                int start = (page - 1) * 10;
                if (start >= regions.size()) {
                    sender.sendMessage(messages.component("region.none-found"));
                    return;
                }
                regions = regions.subList(start, Math.min(regions.size(), start + 10));
                Component[] msgs = new Component[regions.size()];
                int i = 0;
                for (Region region : regions) {
                    Component hoverText = regionManager.withWorldReadLock(region.getWorld(), () -> {
                        boolean showMembers = perm || (uuid != null && region.isAdmin(uuid));
                        return Messages.regionInfo(region, showMembers);
                    });
                    msgs[i] = Component.newline()
                            .append(messages.component("region.info.name",
                                            Placeholders.builder().string("name", region.getName()).build())
                                    .hoverEvent(HoverEvent.showText(hoverText))
                                    .clickEvent(ClickEvent.runCommand("/zone info " + region.getKey())));
                    i++;
                }
                Component msg = Component.textOfChildren(msgs);
                sender.sendMessage(msg);
            });
}
