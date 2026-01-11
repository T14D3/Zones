package de.t14d3.zones.fabric.commands;


import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import net.minecraft.commands.CommandSourceStack;

public class SubCreateCommand {
    private final ZonesFabric mod;
    private final MessageFormatService messages;

    public SubCreateCommand(ZonesFabric mod) {
        this.mod = mod;
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendMessage(messages.component("commands.only-player"));
            return 0;
        }

        RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
        if (player == null) return 0;

        Box selection = player.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
        boolean selecting = player.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false);

        if (selection == null || !selecting) {
            player.extras().put(ZonesExtraKeys.SELECTION, new Box(player.worldOrThrow().ref()));
            player.extras().put(ZonesExtraKeys.SELECTION_CREATING, true);
            player.sendMessage(messages.component("commands.create.click-corners"));
            return 1;
        }

        if (selection.getMin() == null || selection.getMax() == null) {
            player.sendMessage(messages.component("commands.create.click-corners"));
            return 1;
        }

        RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : player.worldOrThrow().ref();
        var regionManager = mod.getRegionManager();

        Region parentRegion = null;
        try {
            String key = context.getArgument("key", String.class);
            RegionKey regionKey = RegionKey.fromString(key);
            Region tempRegion = regionManager.regions().get(regionKey.getValue());
            if (tempRegion != null && regionManager.withWorldReadLock(tempRegion.getWorld(),
                    () -> tempRegion.isAdmin(player.uuid()))) {
                parentRegion = tempRegion;
            } else {
                player.sendMessage(messages.component("commands.invalid-region"));
                return 1;
            }
        } catch (Exception ignored) {
            RBlockPos pos = player.locationOrThrow().blockPos();
            parentRegion = regionManager.withWorldReadLock(selectionWorld, () -> {
                for (Region region : regionManager.getRegionsAt(pos, selectionWorld)) {
                    if (region.isAdmin(player.uuid())) return region;
                }
                return null;
            });
        }

        if (parentRegion == null) {
            player.sendMessage(messages.component("commands.subcreate.no-parent"));
            return 1;
        }

        Region finalParentRegion = parentRegion;
        boolean insideParent = regionManager.withWorldReadLock(finalParentRegion.getWorld(), () ->
                finalParentRegion.contains(selection.getMin()) && finalParentRegion.contains(selection.getMax()));
        if (!insideParent) {
            player.sendMessage(messages.component("commands.subcreate.outside-parent"));
            return 1;
        }

        Region created = regionManager.createSubRegion(
                finalParentRegion.getName() + "_sub",
                selection.getMin(),
                selection.getMax(),
                selectionWorld,
                player.uuid(),
                finalParentRegion
        );
        if (created == null) {
            player.sendMessage(messages.component("commands.create.overlap"));
            return 1;
        }

        mod.getPlatform().removeBeacon(player, selectionWorld, selection.getMin());
        mod.getPlatform().removeBeacon(player, selectionWorld, selection.getMax());
        player.extras().remove(ZonesExtraKeys.SELECTION);
        player.extras().put(ZonesExtraKeys.SELECTION_CREATING, false);

        player.sendMessage(messages.component("commands.subcreate.success"));
        return 1;
    }
}
