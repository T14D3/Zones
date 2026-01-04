package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import net.minecraft.commands.CommandSourceStack;

public class DeleteCommand {
    private final RegionManager regionManager;
    private final MessageFormatService messages;

    public DeleteCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendMessage(messages.component("commands.only-player"));
            return 0;
        }

        RegionKey key = RegionKey.fromString(context.getArgument("key", String.class));
        Region region = regionManager.regions().get(key.getValue());
        if (region == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }

        if (!CommandPermissions.check(context.getSource(), "zones.delete.other")) {
            if (!regionManager.withWorldReadLock(region.getWorld(),
                    () -> region.isAdmin(context.getSource().getPlayer().getUUID()))) {
                context.getSource().sendMessage(messages.component("commands.invalid-region"));
                return 1;
            }
        }

        regionManager.deleteRegion(region.getKey());
        context.getSource().sendMessage(messages.component("commands.delete.success",
                Placeholders.builder().string("region", region.getKey().toString()).build()));
        return 1;
    }
}
