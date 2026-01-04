package de.t14d3.zones.fabric.commands;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Zones;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric command permission checks.
 *
 * <p>Uses vanilla permission levels (via {@code permissions.json}) as a fallback, and
 * delegates to RapunzelLib's {@link RPlayer#hasPermission(String)} for node-based
 * permissions when available.</p>
 */
public final class CommandPermissions {
    private CommandPermissions() {
    }

    public static boolean check(CommandSourceStack source, String permission) {
        if (source == null || permission == null || permission.isBlank()) return false;

        // First try vanilla permission level (permissions.json), then fall back to node-based checks for players.
        int level = 4;
        Zones zones = Zones.getInstance();
        if (zones != null) {
            var manager = zones.getPermissionManager();
            if (manager != null) {
                for (var p : manager.getPermissions()) {
                    if (p != null && permission.equalsIgnoreCase(p.name())) {
                        level = p.level();
                        break;
                    }
                }
            }
        }
        if (source.hasPermission(level)) return true;

        ServerPlayer player = source.getPlayer();
        if (player == null) return false;

        return RPlayer.wrap(player).map(p -> p.hasPermission(permission)).orElse(false);
    }

}
