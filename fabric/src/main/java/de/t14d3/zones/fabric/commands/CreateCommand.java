package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.FabricPlatform;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import net.minecraft.commands.CommandSourceStack;

public class CreateCommand {
    private final RegionManager regionManager;
    private final MessageFormatService messages;
    private final FabricPlatform platform;

    public CreateCommand(ZonesFabric mod) {
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
        this.platform = mod.getPlatform();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            return 0;
        }

        var nativePlayer = context.getSource().getPlayer();
        RPlayer player = RPlayer.wrap(nativePlayer).orElse(null);
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

        if (regionManager.overlapsExistingRegion(selection) && !CommandPermissions.check(context.getSource(),
                "zones.create.overlap")) {
            player.sendMessage(messages.component("commands.create.overlap"));
            return 1;
        }

        RegionPermissions permissions = new RegionPermissions();

        RegionKey key = RegionKey.generate();
        var created = regionManager.createNewRegion(
                key.toString(),
                selection.getMin(),
                selection.getMax(),
                selectionWorld,
                permissions,
                key,
                null,
                0
        );
        if (created != null) {
            regionManager.withWorldWriteLock(created.getWorld(), () -> {
                created.getMembership().player(player.uuid()).roles().allow("owner");
                regionManager.saveRegion(created.getKey(), created);
            });
        }

        platform.removeBeacon(player, selectionWorld, selection.getMin());
        platform.removeBeacon(player, selectionWorld, selection.getMax());
        player.extras().remove(ZonesExtraKeys.SELECTION);
        player.extras().put(ZonesExtraKeys.SELECTION_CREATING, false);

        player.sendMessage(messages.component("commands.create.success",
                Placeholders.builder().string("region", key.toString()).build()));
        return 1;
    }
}
