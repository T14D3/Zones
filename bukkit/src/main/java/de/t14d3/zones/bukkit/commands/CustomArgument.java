package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CustomArgument {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static Argument<Region> region(String name, String permission, MemberType type) {
        return new dev.jorel.commandapi.arguments.CustomArgument<Region, String>(new StringArgument(name), info -> {
            Region region = Zones.getInstance().getRegionManager().regions()
                    .get(RegionKey.fromString(info.input()).getValue());
            if (region != null) {
                if (info.sender() instanceof org.bukkit.entity.Player player) {
                    switch (type) {
                        case OWNER:
                            if (region.isOwner(player.getUniqueId())) {
                                return region;
                            }
                            break;
                        case ADMIN:
                            if (region.isAdmin(player.getUniqueId())) {
                                return region;
                            }
                            break;
                        case MEMBER:
                            if (region.isMember(player.getUniqueId())) {
                                return region;
                            }
                            break;
                        default:
                            return region;
                    }
                } else if (info.sender().hasPermission(permission)) {
                    return region;
                }
            }
            return null;
        }).replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
            return CompletableFuture.supplyAsync(() -> {
                List<Region> regions = new ArrayList<>();
                if (info.sender().hasPermission(permission)) {
                    regions.addAll(Zones.getInstance().getRegionManager().regions().values());
                } else if (info.sender() instanceof org.bukkit.entity.Player player) {
                    for (Region region : Zones.getInstance().getRegionManager().regions().values()) {
                        if (region.isMember(player.getUniqueId())) {
                            regions.add(region);
                        }
                    }
                }
                StringTooltip[] suggestions = new StringTooltip[regions.size()];
                int i = 0;
                for (Region region : regions) {
                    suggestions[i++] = StringTooltip.ofMessage(region.getKey().toString(),
                            BukkitTooltip.messageFromAdventureComponent(
                                    Messages.regionInfo(region, false)));
                }
                return suggestions;
            });
        }));
    }

    public enum MemberType {
        OWNER,
        ADMIN,
        MEMBER,
        ANY
    }
}
