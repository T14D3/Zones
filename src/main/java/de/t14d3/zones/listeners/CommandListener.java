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
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.util.HashMap;
import java.util.Map;

import static de.t14d3.zones.PermissionManager.hasPermission;
import static de.t14d3.zones.Utils.resetBeacon;


public class CommandListener implements CommandExecutor {

    private Zones plugin;
    private RegionManager regionManager;
    private Utils utils;

    public CommandListener(Zones plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.utils = plugin.getUtils();
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
                        if (!hasPermission(player.getUniqueId(), "owner", "true", region)) {
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
                        if (regionManager.overlapsExistingRegion(selectionPair.first(), selectionPair.second()) && !player.hasPermission("zones.create.overlap")) {
                            sender.sendMessage("Error: Region overlaps existing region.");
                            return true;
                        }


                        Map<String, String> perms = new HashMap<>();
                        perms.put("owner", "true");
                        perms.put("break", "true");
                        perms.put("place", "true");
                        perms.put("container", "true");

                        regionManager.create2DRegion(player.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId());
                        resetBeacon(player, selectionPair.first());
                        resetBeacon(player, selectionPair.second());
                        sender.sendMessage("Region '" + player.getName() + "' created successfully!");
                        plugin.selection.remove(player.getUniqueId());
                        return true;
                    }

                case "cancel":
                    if (plugin.selection.containsKey(player.getUniqueId())) {
                        Pair<Location, Location> selection = plugin.selection.get(player.getUniqueId());
                        resetBeacon(player, selection.first());
                        resetBeacon(player, selection.second());
                        plugin.selection.remove(player.getUniqueId());
                        player.sendMessage("Cancelled");
                    }
                    return true;

                case "list":
                    for (Map.Entry<String, RegionManager.Region> entry : regions.entrySet()) {
                        RegionManager.Region region = entry.getValue();
                        player.sendMessage(region.getName());
                    }
                    return true;
                case "info":
                    if (args.length < 2) {
                        sender.sendMessage("Usage: /zone info <regionKey>");
                        return true;
                    }

                    // Extract arguments
                    regionKey = args[1];

                    // Check if region with the given key exists
                    if (regions.containsKey(regionKey)) {
                        RegionManager.Region region = regions.get(regionKey);
                        player.sendMessage("Region Name: " + region.getName());
                        player.sendMessage("Min: " + region.getMin());
                        player.sendMessage("Max: " + region.getMax());
                        player.sendMessage("Members: " + region.getMembers());
                        return true;
                    } else {
                        sender.sendMessage("Error: Region with key '" + regionKey + "' does not exist.");
                    }
                    return true;
                case "test":
                    return true;

            }
        }
        return false;
    }


}
