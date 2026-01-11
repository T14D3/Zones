package de.t14d3.zones.fabric.commands;


import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import net.minecraft.commands.CommandSourceStack;

public class SelectCommand {
    private final ZonesFabric mod;
    private final RegionManager regionManager;

    public SelectCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            return 0;
        }

        RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
        if (player == null) return 0;

        Box selection = player.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
        boolean selecting = player.extras().get(ZonesExtraKeys.SELECTION_CREATING).orElse(false);
        if (selecting && selection != null) {
            RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : player.worldOrThrow()
                    .ref();
            mod.getPlatform().removeBeacon(player, selectionWorld, selection.getMin());
            mod.getPlatform().removeBeacon(player, selectionWorld, selection.getMax());
            player.extras().put(ZonesExtraKeys.SELECTION_CREATING, false);
        }

        Region region = null;
        try {
            String key = context.getArgument("key", String.class);
            region = regionManager.regions().get(RegionKey.fromString(key).getValue());
        } catch (Exception ignored) {
        }

        RBlockPos pos = player.locationOrThrow().blockPos();
        RWorldRef world = player.worldOrThrow().ref();
        if (region == null) {
            region = regionManager.getEffectiveRegionAt(pos, world);
        }

        if (region == null || (selection != null && region.getBounds().equals(selection))) {
            player.extras().remove(ZonesExtraKeys.SELECTION);
            player.sendMessage(mod.getMessages().component("commands.select.deselected"));
        } else {
            player.extras().put(ZonesExtraKeys.SELECTION, region.getBounds());
            player.sendMessage(mod.getMessages().component("commands.select.selected",
                    Placeholders.builder().string("region", region.getName()).build()));
        }
        return 1;
    }
}

