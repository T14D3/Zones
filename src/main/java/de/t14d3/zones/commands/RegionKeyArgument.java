package de.t14d3.zones.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

@SuppressWarnings("UnstableApiUsage")
public class RegionKeyArgument implements CustomArgumentType.Converted<String, String> {
    public RegionKeyArgument() {

    }


    /**
     * Get a region object from a string
     *
     * @param nativeType native argument provided value
     * @return Region object
     * @throws CommandSyntaxException if region is not found
     */
    @Override
    public @NotNull String convert(@NotNull String nativeType) throws CommandSyntaxException {
        String string = nativeType;
        try {
            int key = RegionKey.fromString(nativeType).getValue();
            string = Zones.getInstance().getRegionManager().regions().get(key).getKey().toString();
        } catch (Exception ignored) {
        }
        return string;
    }

    @Override
    public @NotNull ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    static Component regionInfo(Region region) {
        Messages messages = Zones.getInstance().getMessages();
        var mm = MiniMessage.miniMessage();
        Component comp = Component.empty();
        comp = comp.append(
                mm.deserialize("<light_purple>Name: </light_purple>" + messages.get("region.info.name") + " ",
                        parsed("name", region.getName())));
        if (region.getParent() != null) {
            comp = comp.append(mm.deserialize(messages.get("region.info.parent") + " ",
                    parsed("parent", region.getParent().toString())));
        }
        comp = comp.append(mm.deserialize("<green>(</green>" + messages.get("region.info.min") + " - ",
                parsed("min", region.getMinString())));
        comp = comp.append(mm.deserialize(messages.get("region.info.max") + "<green>)</green>",
                parsed("max", region.getMaxString())));
        comp = comp.append(Component.text(" Members: ").color(NamedTextColor.LIGHT_PURPLE));

        // Iterate over members to format permissions
        for (Map.Entry<String, Map<String, String>> member : region.getMembers().entrySet()) {
            String playerName = null;
            try {
                playerName = Utils.getPlayerName(UUID.fromString(member.getKey()));
            } catch (IllegalArgumentException ignored) {
            }
            if (playerName == null) {
                playerName = member.getKey();
            }
            Component playerComponent = mm.deserialize(messages.get("region.info.members.name") + " ",
                    parsed("name", playerName));
            comp = comp.append(playerComponent);
        }

        comp = comp.append(
                mm.deserialize(" " + messages.get("region.info.key"), parsed("key", region.getKey().toString())));
        return comp;
    }
}