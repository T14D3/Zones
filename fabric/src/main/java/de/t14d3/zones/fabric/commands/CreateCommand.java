package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.FabricPlatform;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.command.ServerCommandSource;

import java.util.HashMap;
import java.util.Map;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class CreateCommand {
    private final RegionManager regionManager;
    private final Messages messages;
    private final FabricPlatform platform;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CreateCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
        this.platform = mod.getPlatform();
    }

    int execute(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getPlayer() != null) {
            Player player = platform.getPlayer(context.getSource().getPlayer().getUuid());
            if (player.getSelection() == null) {
                player.setSelection(new Box(platform.getWorld(context.getSource().getWorld())));
                player.setSelectionCreating(true);
                player.sendMessage(messages.getCmp("commands.create.click-corners"));
                return 1;
            }
            Box selection = player.getSelection();
            if (selection.getMin() != null && selection.getMax() != null) {
                if (regionManager.overlapsExistingRegion(selection)
                        && !Permissions.check(context.getSource(), "zones.create.overlap")) {
                    player.sendMessage(messages.getCmp("commands.create.overlap"));
                    return 1;
                }
                Map<String, String> ownerPerms = new HashMap<>();
                ownerPerms.put("role", "owner");
                Map<String, Map<String, String>> members = new HashMap<>();
                members.put(player.getUniqueId().toString(), ownerPerms);
                RegionKey key = RegionKey.generate();
                regionManager.createNewRegion(key.toString(), selection.getMin(),
                        selection.getMax(), selection.getWorld(), members, key, null, 0);
                player.sendMessage(
                        mm.deserialize(messages.get("commands.create.success"),
                                parsed("region", key.toString())));
                player.setSelection(null);
                player.setSelectionCreating(false);
            } else {
                context.getSource().sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
            }
        }
        return 1;
    }
}
