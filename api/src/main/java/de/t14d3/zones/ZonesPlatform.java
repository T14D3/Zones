package de.t14d3.zones;

import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.util.List;
import java.util.UUID;

public interface ZonesPlatform {

    List<World> getWorlds();

    Player getPlayer(UUID uuid);

    Audience getAudience(Player player);

    boolean hasPermission(Player player, String permission);

    ZonesPlatform getPlatform();

    default World getWorld(String world) {
        return getWorlds().stream().filter(w -> w.getName().equals(world)).findFirst().orElse(null);
    }

    File getDataFolder();

    default PermissionManager getPermissionManager() {
        return Zones.getInstance().getPermissionManager();
    }

    Types getTypes();

    World getWorld(Player player);

    BlockLocation getLocation(Player player);

    String getMetadata(Player player, String key);

    void setMetadata(Player player, String key, String value);

    void spawnParticle(int type, BlockLocation particleLocation, Player player);

    void showBeacon(Player player, BlockLocation location, World world, NamedTextColor color);

    void removeBeacon(Player player, World world, BlockLocation location);
}
