package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.commands.utils.CustomArgument;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;

public class SelectCommand {
    private RegionManager regionManager;
    private MessageFormatService messages;
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
                    RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
                    if (rPlayer == null) return;

                    Box selection = rPlayer.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
                    boolean selecting = rPlayer.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false);
                    if (selecting && selection != null) {
                        RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : rPlayer.worldOrThrow()
                                .ref();
                        plugin.getPlatform().removeBeacon(rPlayer, selectionWorld, selection.getMin());
                        plugin.getPlatform().removeBeacon(rPlayer, selectionWorld, selection.getMax());
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION_CREATING, false);
                    }
                    if (args.get("key") == null) {
                        RBlockPos pos = rPlayer.locationOrThrow().blockPos();
                        RWorldRef world = rPlayer.worldOrThrow().ref();
                        region = regionManager.getEffectiveRegionAt(pos, world);
                        if (region == null || (selection != null && region.getBounds().equals(selection))) {
                            rPlayer.extras().remove(ZonesExtraKeys.SELECTION);
                            player.sendMessage(messages.component("commands.select.deselected"));
                            return;
                        }
                    } else {
                        region = regionManager.regions().get(RegionKey.fromString(args.getRaw("key")).getValue());
                    }
                    if (region == null) {
                        player.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }
                    if (selection == null || args.get("key") == null) {
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION, region.getBounds());
                        player.sendMessage(messages.component(
                                "commands.select.selected",
                                Placeholders.builder().string("region", region.getName()).build()
                        ));
                    } else {
                        rPlayer.extras().remove(ZonesExtraKeys.SELECTION);
                        player.sendMessage(messages.component("commands.select.deselected"));
                    }
                }
            });
}
