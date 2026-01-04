package de.t14d3.zones.bukkit.commands.utils;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CustomArgumentSuggestion implements ArgumentSuggestions<CommandSender> {

    private final String permission;
    private final CustomArgument.MemberType type;

    public CustomArgumentSuggestion(String permission, CustomArgument.MemberType type) {
        this.permission = permission;
        this.type = type;
    }

    @Override
    public CompletableFuture<Suggestions> suggest(SuggestionInfo<CommandSender> info, SuggestionsBuilder builder) throws CommandSyntaxException {
        return CompletableFuture.supplyAsync(() -> {
            List<Region> regions = new ArrayList<>();
            var regionManager = Zones.getInstance().getRegionManager();
            if (info.sender().hasPermission(permission)) {
                regions.addAll(regionManager.regions().values());
            } else if (info.sender() instanceof Player player) {
                for (Region region : regionManager.regions().values()) {
                    boolean accessible = regionManager.withWorldReadLock(region.getWorld(), () -> switch (type) {
                        case OWNER -> region.isOwner(player.getUniqueId());
                        case ADMIN -> region.isAdmin(player.getUniqueId());
                        case MEMBER, ANY -> region.isMember(player.getUniqueId());
                    });
                    if (accessible) {
                        regions.add(region);
                    }
                }
            }

            // Filter based on remaining input (case insensitive)
            String remaining = builder.getRemaining().toLowerCase();
            for (Region region : regions) {
                String key = region.getKey().toString();
                String name = region.getName();
                if (key.toLowerCase().contains(remaining) || (name != null && name.toLowerCase().contains(remaining))) {
                    builder.suggest(key,
                            BukkitTooltip.messageFromAdventureComponent(Messages.regionInfo(region, false)));
                }
            }

            return builder.build();
        });
    }
}
