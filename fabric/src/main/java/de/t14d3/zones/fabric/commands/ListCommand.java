package de.t14d3.zones.fabric.commands;


import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.utils.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.UUID;

public class ListCommand {
    private final RegionManager regionManager;
    private final MessageFormatService messages;

    public ListCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context, int page) {
        boolean perm = CommandPermissions.check(context.getSource(), "zones.info.other");
        if (page < 1) page = 1;

        UUID uuid = context.getSource().getPlayer() != null ? context.getSource().getPlayer().getUUID() : null;
        List<Region> regions = regionManager.regions().values().stream()
                .filter(region -> perm || (uuid != null && regionManager.withWorldReadLock(region.getWorld(),
                        () -> region.isMember(uuid))))
                .toList();

        if (regions.isEmpty()) {
            context.getSource().sendMessage(messages.component("region.none-found"));
            return 1;
        }

        int from = (page - 1) * 10;
        if (from >= regions.size()) from = 0;
        regions = regions.subList(from, Math.min(regions.size(), from + 10));

        Component[] msgs = new Component[regions.size()];
        for (int i = 0; i < regions.size(); i++) {
            Region region = regions.get(i);
            Component hoverText = regionManager.withWorldReadLock(region.getWorld(), () -> {
                boolean canSeeAdminInfo = perm || (uuid != null && region.isAdmin(uuid));
                return Messages.regionInfo(region, canSeeAdminInfo);
            });
            msgs[i] = Component.newline()
                    .append(messages.component("region.info.name",
                                    Placeholders.builder().string("name", region.getName()).build())
                            .hoverEvent(HoverEvent.showText(hoverText))
                            .clickEvent(ClickEvent.runCommand("/zone info " + region.getKey()))
                    );
        }

        context.getSource().sendMessage(Component.textOfChildren(msgs));
        return 1;
    }
}

