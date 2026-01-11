package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import net.minecraft.commands.CommandSourceStack;

public class RenameCommand {
    private final RegionManager regionManager;
    private final MessageFormatService messages;

    public RenameCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        Region region = regionManager.regions()
                .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue());
        if (region == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }

        if (!CommandPermissions.check(context.getSource(), "zones.rename.other")) {
            if (context.getSource().getPlayer() == null
                    || !regionManager.withWorldReadLock(region.getWorld(),
                    () -> region.isOwner(context.getSource().getPlayer().getUUID()))) {
                context.getSource().sendMessage(messages.component("commands.invalid-region"));
                return 1;
            }
        }
        String name = context.getArgument("New Name", String.class);
        regionManager.withWorldWriteLock(region.getWorld(), () -> region.setName(name, regionManager));
        context.getSource().sendMessage(messages.component("commands.rename.success",
                Placeholders.builder()
                        .string("region", region.getKey().toString())
                        .string("name", name)
                        .build()));
        return 1;
    }
}
