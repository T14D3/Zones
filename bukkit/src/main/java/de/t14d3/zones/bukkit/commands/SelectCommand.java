package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class SelectCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private ZonesBukkit plugin;

    public SelectCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand select = new CommandAPICommand("select")
            .withPermission("zones.select")
            .withOptionalArguments(CustomArgument.region("key", "zones.set.other", CustomArgument.MemberType.MEMBER))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    Region region;
                    de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUniqueId());
                    if (zplayer.isSelectionCreating()) {
                        plugin.getPlatform().removeBeacon(zplayer, zplayer.getSelection().getWorld(),
                                zplayer.getSelection().getMin());
                        plugin.getPlatform().removeBeacon(zplayer, zplayer.getSelection().getWorld(),
                                zplayer.getSelection().getMax());
                        zplayer.setSelectionCreating(false);
                    }
                    if (args.get("key") == null) {
                        region = regionManager.getEffectiveRegionAt(BlockLocation.of(player.getLocation()),
                                World.of(player.getWorld()));
                        if (region == null || region.getBounds().equals(zplayer.getSelection())) {
                            zplayer.setSelection(null);
                            player.sendMessage(mm.deserialize(messages.get("commands.select.deselected")));
                            return;
                        }
                    } else {
                        region = regionManager.regions().get(RegionKey.fromString(args.getRaw("key")).getValue());
                    }
                    if (region == null) {
                        player.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                        return;
                    }
                    if (zplayer.getSelection() == null || args.get("key") == null) {
                        zplayer.setSelection(region.getBounds());
                        player.sendMessage(mm.deserialize(messages.get("commands.select.selected"),
                                parsed("region", region.getName())));
                    } else {
                        zplayer.setSelection(null);
                        player.sendMessage(mm.deserialize(messages.get("commands.select.deselected")));
                    }
                }
            });
}
