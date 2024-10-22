package de.t14d3.zones.listeners;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.Utils;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

import static de.t14d3.zones.Utils.createBeacon;

public class CommandListener implements CommandExecutor {

    private Zones plugin;
    private RegionManager regionManager;

    public CommandListener(Zones plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("zone") && args.length > 0) {


            Player player = (Player) sender;

            Map<String, RegionManager.Region> regions = regionManager.loadRegions();

            //switch for first subcommand
            switch (args[0].toLowerCase()) {
                case "createmanual":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can use this command.");
                    return true;
                }



                // Validate number of arguments
                if (args.length < 7) {
                    sender.sendMessage("Usage: /zone create <regionName> <minX> <minY> <minZ> <maxX> <maxY> <maxZ>");
                    return true;
                }

                // Extract arguments
                String regionName = args[1];
                String worldName = player.getWorld().toString();

                try {
                    double minX = Double.parseDouble(args[2]);
                    double minY = Double.parseDouble(args[3]);
                    double minZ = Double.parseDouble(args[4]);
                    double maxX = Double.parseDouble(args[5]);
                    double maxY = Double.parseDouble(args[6]);
                    double maxZ = Double.parseDouble(args[7]);

                    Location minLocation = new Location(Bukkit.getWorld(worldName), minX, minY, minZ);
                    Location maxLocation = new Location(Bukkit.getWorld(worldName), maxX, maxY, maxZ);

                    // Create the region
                    regionManager.createNewRegion(regionName, minLocation, maxLocation, player.getUniqueId());

                    sender.sendMessage("Region '" + regionName + "' created successfully!");

                    regionManager.saveRegions();

                } catch (NumberFormatException e) {
                    sender.sendMessage("Error: Coordinates must be valid numbers.");
                    return true;
                } catch (NullPointerException e) {
                    sender.sendMessage("Error: Invalid world name.");
                    return true;
                }

                case "delete":

                    // Validate number of arguments
                    if (args.length < 2) {
                        sender.sendMessage("Usage: /zone delete <regionKey>");
                        return true;
                    }

                    // Extract arguments
                    String regionKey = args[1];


                    // Check if region with the given key exists
                    if (regions.containsKey(regionKey)) {
                        RegionManager.Region region = regions.get(regionKey);
                        if (!region.hasPermission(player.getUniqueId(), "owner", "true")) {
                            sender.sendMessage("Error: You do not have permission to delete this region.");
                            return true;
                        }
                        regionManager.deleteRegion(regionKey);
                        sender.sendMessage("Region '" + regionKey + "' deleted successfully!");

                        // Save the updated regions to the file
                        regionManager.saveRegions();
                    } else {
                        sender.sendMessage("Error: Region with key '" + regionKey + "' does not exist.");
                    }
                    return true;
                case "create":
                    if (!plugin.selection.containsKey(player.getUniqueId())) {
                        plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
                        sender.sendMessage("Click two corners");
                        return true;
                    }

                    Pair <Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
                    if (!(selectionPair.first() == null) && !(selectionPair.second() == null)) {
                        if (regionManager.overlapsExistingRegion(selectionPair.first(), selectionPair.second()) || player.hasPermission("zones.create.overlap")) {
                            sender.sendMessage("Error: Region overlaps existing region.");
                            return true;
                        }
                        regionManager.create2DRegion(player.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId());
                        sender.sendMessage("Region '" + player.getName() + "' created successfully!");
                        plugin.selection.remove(player.getUniqueId());
                        return true;
                    }

                case "cancel":
                    if (plugin.selection.containsKey(player.getUniqueId())) {
                        plugin.selection.remove(player.getUniqueId());
                        player.sendMessage("Cancelled");
                    }
                case "test":
                    createBeacon(player, player.getLocation(), DyeColor.valueOf(args[1]));
                    return true;

            }
        }
        return false;
    }


}
