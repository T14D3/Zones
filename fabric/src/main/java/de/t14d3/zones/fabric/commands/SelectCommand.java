package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.PlayerRepository;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class SelectCommand {
    private final ZonesFabric mod;
    private final RegionManager regionManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SelectCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayer();
        de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUUID());
        Region region = null;
        try {
            region = regionManager.regions()
                    .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue());
        } catch (Exception ignored) {
        }
        if (region == null) {
            region = regionManager.getEffectiveRegionAt(
                    BlockLocation.of(player.getBlockX(), player.getBlockY(), player.getBlockZ()),
                    mod.getPlatform().getWorld(zplayer));
        }
        if (region == null || region.getBounds().equals(zplayer.getSelection())) {
            zplayer.setSelection(null);
            player.sendMessage(mm.deserialize(mod.getMessages().get("commands.select.deselected")));
        } else {
            zplayer.setSelection(region.getBounds());
            player.sendMessage(mm.deserialize(mod.getMessages().get("commands.select.selected"),
                    parsed("region", region.getName())));
        }
        return 1;
    }
}
