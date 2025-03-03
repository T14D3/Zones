package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class RenameCommand {
    private final RegionManager regionManager;
    private final Messages messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RenameCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        Region region = regionManager.regions()
                .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue());
        if (!Permissions.check(context.getSource(), "zones.rename.other") &&
                (context.getSource().getPlayer() == null || !region.isOwner(
                        context.getSource().getPlayer().getUUID()))) {
            context.getSource().sendMessage(messages.getCmp("commands.invalid-region"));
            return 1;
        }
        String name = context.getArgument("New Name", String.class);
        region.setName(name, regionManager);
        context.getSource().sendMessage(
                mm.deserialize(messages.get("commands.rename.success"),
                        parsed("region", region.getKey().toString()),
                        parsed("name", name))
        );
        return 1;
    }
}
