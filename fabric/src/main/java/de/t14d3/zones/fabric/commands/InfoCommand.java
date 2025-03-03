package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;

public class InfoCommand {
    private final ZonesFabric mod;
    private final RegionManager regionManager;
    private final Messages messages;

    public InfoCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        List<Region> regions = new ArrayList<>();
        Player player = null;
        try {
            regions.add(regionManager.regions()
                    .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue()));
        } catch (Exception ignored) {
            if (context.getSource().getPlayer() != null) {
                player = mod.getPlatform().getPlayer(context.getSource().getPlayer().getUUID());

                BlockLocation location = new BlockLocation(context.getSource().getPlayer().getBlockX(),
                        context.getSource().getPlayer().getBlockY(),
                        context.getSource().getPlayer().getBlockZ());
                World world = (mod.getPlatform()).getWorld(
                        context.getSource().getPlayer().getCommandSenderWorld());
                regions = regionManager.getRegionsAt(location, world);
            } else {
                // Sender is not player and no region key provided
                context.getSource().sendMessage(messages.getCmp("commands.invalid-region"));
                return 1;
            }
        }
        for (Region region : regions) {
            if (Permissions.check(context.getSource(), "zones.info.other")) {
                context.getSource().sendMessage(Messages.regionInfo(region, true));
            } else if (player != null && region.isMember(player.getUniqueId())) {
                context.getSource().sendMessage(Messages.regionInfo(region, region.isAdmin(player.getUniqueId())));
            }
        }
        return 1;
    }
}
