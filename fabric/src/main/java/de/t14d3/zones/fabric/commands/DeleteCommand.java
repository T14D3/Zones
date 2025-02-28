package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.FabricPlatform;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class DeleteCommand {
    private final RegionManager regionManager;
    private final Messages messages;
    private final FabricPlatform platform;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DeleteCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
        this.platform = mod.getPlatform();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() != null) {
            Player player = platform.getPlayer(context.getSource().getPlayer().getUUID());
            Region region = regionManager.regions()
                    .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue());
            if (region == null) {
                context.getSource().sendMessage(messages.getCmp("commands.invalid-region"));
                return 1;
            }
            if (!Permissions.check(context.getSource(), "zones.delete.other")) {
                if (context.getSource().getPlayer() != null && !region.isAdmin(
                        context.getSource().getPlayer().getUUID())) {
                    context.getSource().sendMessage(messages.getCmp("commands.invalid-region"));
                    return 1;
                }
            }
            regionManager.deleteRegion(region.getKey());
            context.getSource().sendMessage(
                    mm.deserialize(messages.get("commands.delete.success"),
                            parsed("region", region.getKey().toString())));
            return 1;
        }
        return 0;
    }
}
