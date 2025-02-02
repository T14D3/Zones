package de.t14d3.zones.integrations;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Flags;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class PlaceholderAPI extends PlaceholderExpansion {
    private final Zones plugin;

    public PlaceholderAPI(Zones plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "T14D3";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "zones";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return List.of("%zones_get_name%",
                "%zones_get_key%",
                "%zones_get_members%",
                "%zones_get_owner%",
                "%zones_get_min%",
                "%zones_get_min_x%",
                "%zones_get_min_y%",
                "%zones_get_min_z%",
                "%zones_get_max%",
                "%zones_get_max_x%",
                "%zones_get_max_y%",
                "%zones_get_max_z%",
                "%zones_get_priority%",
                "%zones_get_parent%",
                "%zones_is_member%",
                "%zones_can_place_hand%",
                "%zones_can_break_target%",
                "%zones_can_<action>_<type>%");
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        List<Region> regions = plugin.getRegionManager().getRegionsAtAsync(player.getLocation()).join();
        if (params.equalsIgnoreCase("get_name")) {
            String name = "";
            if (!regions.isEmpty()) {
                name = regions.get(0).getName();
            }
            return name;
        }
        if (params.equalsIgnoreCase("get_key")) {
            String key = "";
            if (!regions.isEmpty()) {
                key = regions.get(0).getKey().toString();
            }
            return key;
        }
        if (params.equalsIgnoreCase("get_members")) {
            final String[] members = {""};
            regions.get(0).getMembers().keySet().forEach(val -> {
                String member;
                try {
                    UUID uuid = UUID.fromString(val);
                    member = Bukkit.getOfflinePlayer(uuid).getName() != null ? Bukkit.getOfflinePlayer(uuid)
                            .getName() : String.valueOf(uuid);
                } catch (IllegalArgumentException ignored) {
                    member = val;
                }

                if (members[0].isEmpty()) {
                    members[0] = member;
                } else {
                    members[0] = members[0] + ", " + member;
                }
            });
            return members[0];
        }
        if (params.equalsIgnoreCase("get_owner")) {
            String owner = "";
            if (!regions.isEmpty()) {
                if (regions.get(0).getOwner() != null) {
                    owner = Bukkit.getOfflinePlayer(regions.get(0).getOwner()).getName() != null
                            ? Bukkit.getOfflinePlayer(regions.get(0).getOwner()).getName() : "";
                }
            }
            return owner;
        }
        if (params.equalsIgnoreCase("get_min")) {
            if (!regions.isEmpty()) {
                return regions.get(0).getMinString();
            }
            return "";
        }
        if (params.equalsIgnoreCase("get_min_x")) {
            if (!regions.isEmpty()) {
                return String.valueOf(regions.get(0).getMin().getBlockX());
            }
            return "";
        }
        if (params.equalsIgnoreCase("get_min_y")) {
            int minY = 0;
            if (!regions.isEmpty()) {
                minY = regions.get(0).getMin().getBlockY();
            }
            return String.valueOf(minY);
        }
        if (params.equalsIgnoreCase("get_min_z")) {
            if (!regions.isEmpty()) {
                return String.valueOf(regions.get(0).getMin().getBlockZ());
            }
            return "";
        }
        if (params.equalsIgnoreCase("get_max")) {
            if (!regions.isEmpty()) {
                return regions.get(0).getMaxString();
            }
            return "";
        }
        if (params.equalsIgnoreCase("get_max_x")) {
            if (!regions.isEmpty()) {
                return String.valueOf(regions.get(0).getMax().getBlockX());
            }
            return "";
        }
        if (params.equalsIgnoreCase("get_max_y")) {
            if (!regions.isEmpty()) {
                return String.valueOf(regions.get(0).getMax().getBlockY());
            }
            return "";
        }
        if (params.equalsIgnoreCase("get_max_z")) {
            if (!regions.isEmpty()) {
                return String.valueOf(regions.get(0).getMax().getBlockZ());
            }
            return "";
        }
        if (params.equalsIgnoreCase("is_member")) {
            if (!regions.isEmpty()) {
                return regions.get(0).isMember(player.getUniqueId()) ? "true" : "false";
            }
            return "false";
        }
        if (params.equalsIgnoreCase("can_place_hand")) {
            if (!regions.isEmpty()) {
                return plugin.getPermissionManager()
                        .checkAction(player.getLocation(), player.getUniqueId(), Flags.PLACE,
                                player.getInventory().getItemInMainHand().getType().name()) ? "true" : "false";
            }
            return "false";
        }
        if (params.equalsIgnoreCase("can_break_target")) {
            if (!regions.isEmpty()) {
                return plugin.getPermissionManager().checkAction(
                        player.getLocation(),
                        player.getUniqueId(),
                        Flags.BREAK,
                        player.getTargetBlockExact(5) != null
                                ? player.getTargetBlockExact(5).getType().name() : "")
                        ? "true" : "false";
            }
            return "false";
        }
        if (params.startsWith("can_")) {
            String action = params.split("_")[1].toUpperCase();
            // There's definitely a better way to do this, but it works and I'm too lazy to find a better one
            String type = params.substring(params.indexOf('_', params.indexOf('_') + 1) + 1);
            if (!regions.isEmpty()) {
                return plugin.getPermissionManager()
                        .checkAction(player.getLocation(), player.getUniqueId(), Flags.getFlag(action),
                                type) ? "true" : "false";
            }
            return "false";
        }
        if (params.equalsIgnoreCase("get_priority")) {
            if (!regions.isEmpty()) {
                return String.valueOf(regions.get(0).getPriority());
            }
            return "0";
        }
        if (params.equalsIgnoreCase("get_parent")) {
            if (!regions.isEmpty()) {
                return regions.get(0).getParent().toString();
            }
            return "";
        }
        return null;
    }
}
