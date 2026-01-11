package de.t14d3.zones.rapunzellib;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Flag;
import net.kyori.adventure.text.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Utility for sending actionbar messages related to Zones.
 */
public final class ZonesActionbar {
    private static final long COOLDOWN_NANOS = Duration.ofMillis(250).toNanos();

    private ZonesActionbar() {
    }

    /**
     * Sends an actionbar message if the player isn't on cooldown.
     *
     * @param player    target player
     * @param component actionbar component
     * @return {@code true} if the message was sent, {@code false} if throttled
     */
    public static boolean sendThrottled(RPlayer player, Component component) {
        long now = System.nanoTime();
        Long last = player.extras().get(ZonesExtraKeys.LAST_ACTIONBAR_NANOS).orElse(null);
        if (last != null && now - last < COOLDOWN_NANOS) return false;

        player.extras().put(ZonesExtraKeys.LAST_ACTIONBAR_NANOS, now);
        player.sendActionBar(component);
        return true;
    }

    /**
     * Sends a standard "denied" actionbar message listing the regions and missing permissions.
     *
     * <p>Messages are throttled per-player (shared cooldown with {@link #sendThrottled(RPlayer, Component)}).</p>
     */
    public static void sendDenied(
            Zones zones,
            RPlayer player,
            RBlockPos location,
            RWorldRef world,
            List<Flag> actions,
            String type
    ) {
        sendDenied(zones, player, location, world, actions.toArray(Flag[]::new), type);
    }

    /**
     * Same as {@link #sendDenied(Zones, RPlayer, RBlockPos, RWorldRef, List, String)} but avoids list allocations on hot paths.
     */
    public static void sendDenied(
            Zones zones,
            RPlayer player,
            RBlockPos location,
            RWorldRef world,
            Flag[] actions,
            String type
    ) {
        long now = System.nanoTime();
        Long last = player.extras().get(ZonesExtraKeys.LAST_ACTIONBAR_NANOS).orElse(null);
        if (last != null && now - last < COOLDOWN_NANOS) return;

        List<Region> regions = zones.getRegionManager().getRegionsAt(location, world);

        String regionNames;
        if (regions.isEmpty()) {
            regionNames = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < regions.size(); i++) {
                if (i != 0) sb.append(", ");
                sb.append(regions.get(i).getName());
            }
            regionNames = sb.toString();
        }

        String permissionsString;
        if (actions == null || actions.length == 0) {
            permissionsString = "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < actions.length; i++) {
                if (i != 0) sb.append(", ");
                sb.append(actions[i].name());
            }
            permissionsString = sb.toString();
        }

        Component component = Rapunzel.context().messages().component(
                "region.no-interact-permission",
                Placeholders.builder()
                        .string("region", regionNames)
                        .string("actions", permissionsString)
                        .string("type", Objects.toString(type, ""))
                        .build()
        );

        player.extras().put(ZonesExtraKeys.LAST_ACTIONBAR_NANOS, now);
        player.sendActionBar(component);
    }
}
