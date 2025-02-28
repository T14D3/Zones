package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class ListCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;

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

                Player player;
                if (sender instanceof Player temp) {
                    player = temp;
                } else {
                    player = null;
                }
                List<Region> regions = regionManager.regions().values().parallelStream()
                        .filter(region -> perm || (player != null && region.isMember(player.getUniqueId()))).toList();
                if (regions.isEmpty()) {
                    sender.sendMessage(messages.getCmp("region.none-found"));
                    return;
                }
                regions = regions.subList((page - 1) * 10, Math.min(regions.size(), page * 10));
                Component[] msgs = new Component[regions.size()];
                int i = 0;
                for (Region region : regions) {
                    msgs[i] = Component.newline()
                            .append(mm.deserialize(messages.get("region.info.name"), parsed("name", region.getName()),
                                            parsed("key", region.getKey().toString()))
                                    .hoverEvent(HoverEvent.showText(Messages.regionInfo(region,
                                            (perm || region.isAdmin(player.getUniqueId())))))
                                    .clickEvent(ClickEvent.runCommand("/zone info " + region.getKey())));
                    i++;
                }
                Component msg = Component.textOfChildren(msgs);
                sender.sendMessage(msg);
            });
}
