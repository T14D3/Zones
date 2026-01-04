package de.t14d3.zones.bukkit.commands.utils;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;

public class CustomArgument {
    public static Argument<Region> region(String name, String permission, MemberType type) {
        return new dev.jorel.commandapi.arguments.CustomArgument<Region, String>(new StringArgument(name), info -> {
            var regionManager = Zones.getInstance().getRegionManager();
            Region region = regionManager.regions()
                    .get(RegionKey.fromString(info.input()).getValue());
            if (region != null) {
                if (info.sender().hasPermission(permission)) {
                    return region;
                }
                if (!(info.sender() instanceof org.bukkit.entity.Player player)) {
                    return null;
                }
                boolean accessible = regionManager.withWorldReadLock(region.getWorld(), () -> switch (type) {
                    case OWNER -> region.isOwner(player.getUniqueId());
                    case ADMIN -> region.isAdmin(player.getUniqueId());
                    case MEMBER, ANY -> region.isMember(player.getUniqueId());
                });
                if (accessible) {
                    return region;
                }
            }
            return null;
        }).replaceSuggestions(new CustomArgumentSuggestion(permission, type));
    }

    public enum MemberType {
        OWNER,
        ADMIN,
        MEMBER,
        ANY
    }
}
