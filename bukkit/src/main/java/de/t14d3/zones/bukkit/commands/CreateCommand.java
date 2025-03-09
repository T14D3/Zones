package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.objects.RegionFlagEntry;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class CreateCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private ZonesBukkit plugin;

    public CreateCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand create = new CommandAPICommand("create")
            .withPermission("zones.create")
            .withOptionalArguments(new StringArgument("name"))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUniqueId());
                    if (zplayer.getSelection() == null) {
                        zplayer.setSelection(new Box(null, null, World.of(player.getWorld()), false));
                        zplayer.setSelectionCreating(true);
                        sender.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                        return;
                    }
                    Box selection = zplayer.getSelection();
                    if (selection.getMin() != null && selection.getMax() != null) {
                        if (regionManager.overlapsExistingRegion(selection) && !sender.hasPermission(
                                "zones.create.overlap")) {
                            sender.sendMessage(mm.deserialize(messages.get("commands.create.overlap")));
                            return;
                        }
                        Map<String, List<RegionFlagEntry>> members = new HashMap<>();
                        List<RegionFlagEntry> ownerPermissions = new ArrayList<>();
                        ownerPermissions.add(new RegionFlagEntry("role", "owner", false));
                        members.put(player.getUniqueId().toString(), ownerPermissions);
                        RegionKey key = RegionKey.generate();
                        regionManager.createNewRegion(key.toString(), selection.getMin(),
                                selection.getMax(), selection.getWorld(), members, key, null, 0);
                        Utils.SelectionMode mode = Utils.SelectionMode.getPlayerMode(zplayer);

                        plugin.getPlatform().removeBeacon(zplayer, selection.getWorld(), selection.getMin());
                        plugin.getPlatform().removeBeacon(zplayer, selection.getWorld(), selection.getMax());
                        sender.sendMessage(
                                mm.deserialize(messages.get("commands.create.success"),
                                        parsed("region", key.toString())));
                        zplayer.setSelection(null);
                        zplayer.setSelectionCreating(false);
                    } else {
                        sender.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                    }
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.only-player")));
                }
            });
}
