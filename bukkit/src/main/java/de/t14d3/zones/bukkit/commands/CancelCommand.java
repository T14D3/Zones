package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.BukkitPlatform;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.World;
import org.bukkit.entity.Player;

import static de.t14d3.zones.bukkit.visuals.BeaconUtils.resetBeacon;

public class CancelCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final RegionManager regionManager;
    private Messages messages;
    private final ZonesBukkit plugin;

    public CancelCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand cancel = new CommandAPICommand("cancel")
            .withPermission("zones.cancel")
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUniqueId());
                    if (zplayer.getSelection() != null) {
                        World world = BukkitPlatform.getWorld(zplayer.getSelection().getWorld());
                        resetBeacon(player, zplayer.getSelection().getMin().toLocation(world));
                        resetBeacon(player, zplayer.getSelection().getMax().toLocation(world));
                        zplayer.setSelection(null);
                        player.sendMessage(mm.deserialize(messages.get("commands.cancel.success")));
                    } else {
                        player.sendMessage(mm.deserialize(messages.get("commands.cancel.success")));
                    }
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.only-player")));
                }
            });
}