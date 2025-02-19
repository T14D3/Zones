package de.t14d3.zones.commands;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.flags.Flag;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.ListArgumentBuilder;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class SetCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private Zones plugin;

    public SetCommand(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    public CommandAPICommand set = new CommandAPICommand("set")
            .withPermission("zones.set")
            .withArguments(
                    new StringArgument("key")
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    List<Region> regions = new ArrayList<>();
                                    if (info.sender().hasPermission("zones.set.other")) {
                                        regions.addAll(regionManager.regions().values());
                                    } else if (info.sender() instanceof Player player) {
                                        for (Region region : regionManager.regions().values()) {
                                            if (region.isMember(player.getUniqueId())) {
                                                regions.add(region);
                                            }
                                        }
                                    }
                                    StringTooltip[] suggestions = new StringTooltip[regions.size()];
                                    int i = 0;
                                    for (Region region : regions) {
                                        suggestions[i++] = StringTooltip.ofMessage(region.getKey().toString(),
                                                BukkitTooltip.messageFromAdventureComponent(
                                                        Messages.regionInfo(region, false)));
                                    }
                                    return suggestions;
                                });
                            })),
                    new StringArgument("target")
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    Region region = regionManager.regions()
                                            .get(RegionKey.fromString((String) info.previousArgs().get("key"))
                                                    .getValue());
                                    if (region == null) {
                                        return new StringTooltip[0];
                                    } else if (!info.sender().hasPermission("zones.set.other")) {
                                        if (!(info.sender() instanceof Player player && region.isOwner(
                                                player.getUniqueId()))) {
                                            return new StringTooltip[0];
                                        }
                                    }
                                    Map<String, Component> targets = new HashMap<>();
                                    region.getGroupNames().forEach(group -> {
                                        List<String> groupMembers = new ArrayList<>();
                                        region.getGroupMembers(group).forEach(
                                                val -> groupMembers.add(Utils.getPlayerName(UUID.fromString(val))));
                                        targets.put(group, Component.text(groupMembers.toString()));
                                    });
                                    Utils.playerNames.forEach(
                                            (uuid, name) -> targets.put(name, Component.text(uuid.toString())));
                                    targets.put("+universal", (mm.deserialize(messages.get("flags.universal"))));
                                    StringTooltip[] suggestions = new StringTooltip[targets.size()];
                                    int i = 0;
                                    for (Map.Entry<String, Component> target : targets.entrySet()) {
                                        suggestions[i++] = StringTooltip.ofMessage(target.getKey(),
                                                BukkitTooltip.messageFromAdventureComponent(target.getValue()));
                                    }
                                    return suggestions;
                                });
                            })),
                    new StringArgument("flag")
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    List<String> flags = new ArrayList<>();
                                    boolean universal = ((String) info.previousArgs().get("target")).equalsIgnoreCase(
                                            "+universal");
                                    for (Flag flag : Flags.getFlags()) {
                                        if (flag.getDefaultValue() != universal) {
                                            continue;
                                        }
                                        flags.add(flag.name());
                                    }
                                    StringTooltip[] suggestions = new StringTooltip[flags.size()];
                                    int i = 0;
                                    for (String flag : flags) {
                                        suggestions[i++] = StringTooltip.ofMessage(flag,
                                                BukkitTooltip.messageFromAdventureComponent(
                                                        mm.deserialize(messages.get("flags." + flag))));
                                    }
                                    return suggestions;
                                });
                            })),
                    new ListArgumentBuilder<String>("values")
                            .withList(info -> Flags.getFlag((String) info.previousArgs().get("flag")).getValidValues())
                            .withStringMapper()
                            .buildGreedy()
            )
            .executes((sender, args) -> {
                RegionKey regionKey = RegionKey.fromString((String) args.get("key"));
                Region region = regionManager.regions().get(regionKey.getValue());
                if (sender.hasPermission("zones.set.other") || (sender instanceof Player player && region.isOwner(
                        player.getUniqueId()))) {
                    StringBuilder builder = new StringBuilder();
                    List<String> values = (List<String>) args.get("values");
                    for (String value : values) {
                        builder.append(value).append(",");
                    }
                    String display = args.getRaw("target");
                    String target;
                    if (display.startsWith("+")) {
                        target = display;
                    } else {
                        try {
                            target = Bukkit.getOfflinePlayerIfCached(display).getUniqueId().toString();
                        } catch (Exception e) {
                            sender.sendMessage(mm.deserialize(messages.get("commands.invalid-player")));
                            return;
                        }
                    }
                    regionManager.addMemberPermission(target, (String) args.get("flag"),
                            builder.toString(), regionKey);
                    sender.sendMessage(mm.deserialize(messages.get("commands.set.success"),
                            parsed("region", regionKey.toString()),
                            parsed("target", display),
                            parsed("permission", (String) args.get("flag")),
                            parsed("value", builder.toString())));
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.invalid-region")));
                }
            });

}
