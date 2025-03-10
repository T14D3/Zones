package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.*;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class SubCreateCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private ZonesBukkit plugin;

    public SubCreateCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand subcreate = new CommandAPICommand("subcreate")
            .withPermission("zones.subcreate")
            .withOptionalArguments(CustomArgument.region("key", "zones.set.other", CustomArgument.MemberType.ADMIN))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUniqueId());
                    if (zplayer.getSelection() == null) {
                        zplayer.setSelection(new Box(null, null, World.of(player.getWorld()), false));
                        zplayer.setSelectionCreating(true);
                        player.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                        return;
                    }
                    Box selection = zplayer.getSelection();
                    if (selection.getMin() == null || selection.getMax() == null) {
                        player.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                        return;
                    }

                    Region parentRegion = null;
                    if (args.get("key") == null) {
                        for (Region region : regionManager.getRegionsAt(BlockLocation.of(player.getLocation()),
                                World.of(player.getWorld()))) {
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

                    if (!parentRegion.contains(selection.getMin())
                            || !parentRegion.contains(selection.getMax())) {
                        player.sendMessage(mm.deserialize(messages.get("commands.subcreate.outside-parent")));
                        return; // Failure
                    }

                    List<RegionFlagEntry> perms = new ArrayList<>();
                    perms.add(new RegionFlagEntry("role", "owner", false));

                    regionManager.createSubRegion(parentRegion.getName() + "_sub", selection.getMin(),
                            selection.getMax(), selection.getWorld(), player.getUniqueId(), perms, parentRegion);
                    plugin.getPlatform().removeBeacon(zplayer, selection.getWorld(), selection.getMin());
                    plugin.getPlatform().removeBeacon(zplayer, selection.getWorld(), selection.getMax());
                    player.sendMessage(mm.deserialize(messages.get("commands.subcreate.success")));
                    zplayer.setSelection(null);
                    zplayer.setSelectionCreating(false);
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.only-player")));
                }
            });
}
