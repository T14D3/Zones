package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.FabricPlatform;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class ListCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final RegionManager regionManager;
    private final Messages messages;
    private final FabricPlatform platform;

    public ListCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
        this.platform = mod.getPlatform();
    }

    int execute(CommandContext<ServerCommandSource> context, int page) {
        boolean perm = Permissions.check(context.getSource(), "zones.info.other");
        if (page < 1) {
            page = 1;
        }
        Player player = context.getSource().getPlayer() != null ? platform.getPlayer(
                context.getSource().getPlayer().getUuid()) : null;
        List<Region> regions = regionManager.regions().values().parallelStream()
                .filter(region -> perm || (player != null && region.isMember(player.getUniqueId())))
                .toList();
        if (regions.isEmpty()) {
            context.getSource().sendMessage(messages.getCmp("region.none-found"));
            return 1;
        }
        regions = regions.subList((page - 1) * 10, Math.min(regions.size(), page * 10));
        Component[] msgs = new Component[regions.size()];
        int i = 0;
        for (Region region : regions) {
            msgs[i] = Component.newline()
                    .append(mm.deserialize(messages.get("region.info.name"),
                                    parsed("name", region.getName()),
                                    parsed("key", region.getKey().toString()))
                            .hoverEvent(HoverEvent.showText(Messages.regionInfo(region,
                                    (perm || region.isAdmin(player.getUniqueId())))))
                            .clickEvent(ClickEvent.runCommand("/zone info " + region.getKey()))
                    );
            i++;
        }
        Component msg = Component.textOfChildren(msgs);
        context.getSource().sendMessage(msg);

        return 1;
    }
}
