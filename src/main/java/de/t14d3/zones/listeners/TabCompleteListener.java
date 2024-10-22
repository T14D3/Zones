package de.t14d3.zones.listeners;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TabCompleteListener implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("zone")) {
            List<String> completions = new ArrayList<String>();
            if (args.length == 1) {
                completions.add("create");
                completions.add("delete");
                completions.add("list");
                completions.add("info");
                completions.add("set");
            } else if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("createmanual")) {
                    if (args.length == 2) {
                        completions.add("name");
                    }
                    if (args.length == 3) {
                        completions.add(player.getLocation().getBlockX() + "");
                    }
                    if (args.length == 4) {
                        completions.add(player.getLocation().getBlockY() + "");
                    }
                    if (args.length == 5) {
                        completions.add(player.getLocation().getBlockZ() + "");
                    }
                    if (args.length == 6) {
                        completions.add(player.getLocation().getBlockX() + "");
                    }
                    if (args.length == 7) {
                        completions.add(player.getLocation().getBlockY() + "");
                    }
                    if (args.length == 8) {
                        completions.add(player.getLocation().getBlockZ() + "");
                    }


                } else if (args[0].equalsIgnoreCase("delete")) {
                    completions.add("name");
                }
            }
            return completions;
        }
        return null;
    }
}
