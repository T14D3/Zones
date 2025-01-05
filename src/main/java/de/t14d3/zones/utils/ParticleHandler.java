package de.t14d3.zones.utils;

import com.destroystokyo.paper.ParticleBuilder;
import de.t14d3.zones.Zones;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ParticleHandler {
    private final Zones plugin;

    public ParticleHandler(Zones plugin) {
        this.plugin = plugin;

    }


    public void particleScheduler() {
        Particle primary = Particle.valueOf(plugin.getConfig().getString("selection-particles.primary", "WAX_OFF"));
        Particle secondary = Particle.valueOf(plugin.getConfig().getString("selection-particles.secondary", "WAX_ON"));
        double range = plugin.getConfig().getDouble("selection-particles.range", 15);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, BoundingBox> entry : plugin.particles.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    BoundingBox box = entry.getValue();
                    if (player != null) {
                        spawnParticleOutline(
                                primary,
                                secondary,
                                player,
                                new Location(player.getWorld(), box.getMin().getBlockX(), box.getMin().getBlockY(), box.getMin().getBlockZ()),
                                new Location(player.getWorld(), box.getMax().getBlockX(), box.getMax().getBlockY(), box.getMax().getBlockZ()),
                                range);
                    }

                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 2);
    }

    static void spawnParticleOutline(Particle primary, Particle secondary, Player player, Location min, Location max, double range) {
        // Get the corners of the box
        double x1 = min.getX();
        double y1 = min.getY();
        double z1 = min.getZ();
        double x2 = max.getX();
        double y2 = max.getY();
        double z2 = max.getZ();

        // Create a list of locations for the outline
        List<Location> outlineLocations = new ArrayList<>();

        // Add corners to the outline
        outlineLocations.add(new Location(player.getWorld(), x1, y1, z1));
        outlineLocations.add(new Location(player.getWorld(), x2, y1, z1));
        outlineLocations.add(new Location(player.getWorld(), x2, y1, z2));
        outlineLocations.add(new Location(player.getWorld(), x1, y1, z2));

        ParticleBuilder particleBuilder = new ParticleBuilder(primary);
        ParticleBuilder particleBuilder2 = new ParticleBuilder(secondary);

        for (int i = 0; i < outlineLocations.size(); i++) {
            Location start = outlineLocations.get(i);
            Location end = outlineLocations.get((i + 1) % outlineLocations.size()); // Loop back to the first point

            // Calculate the distance between start and end
            double distance = start.distance(end);

            // Calculate the number of particles needed based on the specified spacing
            int numParticles = (int) (distance);
            Vector direction = end.toVector().subtract(start.toVector()).normalize();

            // Spawn particles along the line
            for (int j = 0; j <= numParticles; j++) {
                for (int k = (int) y1; k <= y2; k++) {
                    Location particleLocation = start.clone().add(direction.clone().multiply(j));
                    particleLocation.setY(k);
                    if (particleLocation.distance(player.getLocation()) < range) {
                        particleBuilder.location(particleLocation);
                        particleBuilder.count(1);
                        particleBuilder.extra(0);
                        particleBuilder.force(true);
                        particleBuilder.spawn();
                    }
                }
            }
        }

        for (double y = y1; y <= y2; y++) {
            for (Location corner : outlineLocations) {
                Location particleLocation = new Location(player.getWorld(), corner.getX(), y, corner.getZ());
                if (particleLocation.distance(player.getLocation()) < range) {
                    particleBuilder2.location(particleLocation);
                    particleBuilder2.count(1);
                    particleBuilder2.extra(0);
                    particleBuilder2.force(true);
                    particleBuilder2.spawn();
                }
            }
        }
    }
}
