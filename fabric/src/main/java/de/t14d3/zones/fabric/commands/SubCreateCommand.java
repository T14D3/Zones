package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.fabric.FabricPlatform;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.objects.RegionFlagEntry;
import de.t14d3.zones.utils.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class SubCreateCommand {
    private final ZonesFabric mod;
    private final FabricPlatform platform;
    private final Messages messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SubCreateCommand(ZonesFabric mod) {
        this.mod = mod;
        this.platform = mod.getPlatform();
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() != null) {
            de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(context.getSource().getPlayer().getUUID());
            if (zplayer.getSelection() == null) {
                zplayer.setSelection(
                        new Box(null, null, platform.getWorld(context.getSource().getPlayer().serverLevel()), false));
                zplayer.setSelectionCreating(true);
                zplayer.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                return 1;
            }
            Box selection = zplayer.getSelection();
            if (selection.getMin() == null || selection.getMax() == null) {
                zplayer.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                return 1;
            }

            Region parentRegion = null;
            try {
                context.getArgument("key", String.class);
                RegionKey regionKey = RegionKey.fromString(context.getArgument("key", String.class));
                Region tempRegion = mod.getRegionManager().regions().get(regionKey.getValue());
                if (tempRegion == null || !tempRegion.isAdmin(context.getSource().getPlayer().getUUID())) {
                    zplayer.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                    return 1;
                }
            } catch (Exception e) {
                for (Region region : mod.getRegionManager().getRegionsAt(
                        BlockLocation.of(context.getSource().getPlayer().getBlockX(),
                                context.getSource().getPlayer().getBlockY(),
                                context.getSource().getPlayer().getBlockZ()),
                        platform.getWorld(context.getSource().getPlayer().serverLevel()))) {
                    if (region.isAdmin(context.getSource().getPlayer().getUUID())) {
                        parentRegion = region;
                        break;
                    }
                }
            }
            if (parentRegion == null) {
                zplayer.sendMessage(mm.deserialize(messages.get("commands.subcreate.no-parent")));
                return 1;
            }

            if (!parentRegion.contains(selection.getMin())
                    || !parentRegion.contains(selection.getMax())) {
                zplayer.sendMessage(mm.deserialize(messages.get("commands.subcreate.outside-parent")));
                return 1;
            }

            mod.getRegionManager().createSubRegion(parentRegion.getName() + "_sub", selection.getMin(),
                    selection.getMax(), selection.getWorld(), zplayer.getUniqueId(),
                    List.of(new RegionFlagEntry("role", "owner", false)), parentRegion);
            mod.getPlatform().removeBeacon(zplayer, selection.getWorld(), selection.getMin());
            mod.getPlatform().removeBeacon(zplayer, selection.getWorld(), selection.getMax());
            zplayer.sendMessage(mm.deserialize(messages.get("commands.subcreate.success")));
            zplayer.setSelection(null);
            zplayer.setSelectionCreating(false);
        } else {
            context.getSource().sendMessage(mm.deserialize(messages.get("commands.only-player")));
        }

        return 1;
    }
}
