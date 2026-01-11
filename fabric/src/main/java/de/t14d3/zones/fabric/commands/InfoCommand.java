package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.utils.Messages;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InfoCommand {
    private final ZonesFabric mod;
    private final RegionManager regionManager;
    private final MessageFormatService messages;

    public InfoCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        List<Region> regions = new ArrayList<>();
        UUID playerUuid = null;

        try {
            Region region = regionManager.regions()
                    .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue());
            if (region != null) {
                regions.add(region);
            }
        } catch (Exception ignored) {
        }

        if (regions.isEmpty()) {
            if (context.getSource().getPlayer() == null) {
                context.getSource().sendMessage(messages.component("commands.invalid-region"));
                return 1;
            }

            RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
            if (player == null) return 0;
            playerUuid = player.uuid();

            RBlockPos pos = player.locationOrThrow().blockPos();
            RWorldRef world = player.worldOrThrow().ref();
            regions = regionManager.getRegionsAt(pos, world);
        }

        for (Region region : regions) {
            if (CommandPermissions.check(context.getSource(), "zones.info.other")) {
                context.getSource().sendMessage(
                        regionManager.withWorldReadLock(region.getWorld(), () -> Messages.regionInfo(region, true)));
            } else if (playerUuid != null) {
                UUID uuid = playerUuid;
                var msg = regionManager.withWorldReadLock(region.getWorld(), () -> {
                    if (!region.isMember(uuid)) return null;
                    return Messages.regionInfo(region, region.isAdmin(uuid));
                });
                if (msg != null) context.getSource().sendMessage(msg);
            }
        }
        return 1;
    }
}
