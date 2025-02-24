package de.t14d3.zones;

import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;

import java.io.File;
import java.util.List;
import java.util.UUID;

public interface ZonesPlatform {

    List<World> getWorlds();

    Player getPlayer(UUID uuid);


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
}
