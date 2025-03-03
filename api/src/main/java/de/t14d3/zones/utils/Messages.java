package de.t14d3.zones.utils;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class Messages {
    private final Zones zones;
    private final Map<String, String> messages = new HashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public Messages(Properties messagesConfig, Zones zones) {
        this.zones = zones;
        messagesConfig.keySet().forEach(
                key ->
                        messages.put(key.toString(), messagesConfig.getProperty(key.toString()))
        );
    }

    /**
     * Gets a message from the messages.yml file.
     * Defaults to "messages.default" if not found.
     *
     * @param key The key of the message.
     * @return The message.
     */
    public @NotNull String get(String key) {
        return messages.getOrDefault(key,
                zones.getConfig().getString("messages.default", key).replaceAll("<key>", key));
    }

    public @NotNull Component getCmp(String key) {
        return mm.deserialize(messages.getOrDefault(key,
                zones.getConfig().getString("messages.default", key).replaceAll("<key>", key)));
    }

    public @NotNull String getOrDefault(String key, String defaultValue) {
        return messages.getOrDefault(key, defaultValue);
    }

    public static Component regionInfo(Region region, boolean showMembers) {
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

        if (showMembers) {
            // Iterate over members to format permissions
            for (Map.Entry<String, Map<String, String>> member : region.getMembers().entrySet()) {
                String playerName = null;
                try {
                    playerName = Zones.getInstance().getPlatform().getPlayer(UUID.fromString(member.getKey()))
                            .getName();
                } catch (IllegalArgumentException ignored) {
                }
                if (playerName == null) {
                    playerName = member.getKey();
                }
                Component playerComponent = mm.deserialize(messages.get("region.info.members.name"),
                        parsed("name", playerName));
                playerComponent = playerComponent.appendNewline();

                // Extract permissions and format them using Adventure Components
                Map<String, String> permissions = member.getValue();
                Component permissionsComponent = Component.empty();

                for (Map.Entry<String, String> permEntry : permissions.entrySet()) {
                    String permKey = permEntry.getKey();
                    String permValue = permEntry.getValue();

                    // Split permission values by comma and trim whitespace
                    String[] permValues = permValue.split(",\\s*");
                    List<Component> formattedComponents = new ArrayList<>();

                    for (String value : permValues) {
                        Component formattedValue;
                        if ("true".equalsIgnoreCase(value) || "*".equals(value)) {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.allowed"),
                                    parsed("value", value));
                        } else if ("false".equalsIgnoreCase(value) || value.startsWith("!")) {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.denied"),
                                    parsed("value", value));
                        } else {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.allowed"),
                                    parsed("value", value));
                        }
                        formattedComponents.add(formattedValue);
                    }
                    // Combine all formatted components into one line
                    Component permLine = mm.deserialize(messages.get("region.info.members.permission"),
                            parsed("permission", permKey));

                    // Append all value components with comma separators
                    for (int i = 0; i < formattedComponents.size(); i++) {
                        permLine = permLine.append(formattedComponents.get(i));
                        if (i < formattedComponents.size() - 1) {
                            permLine = permLine.append(Component.text(", ").color(NamedTextColor.GRAY));
                        }
                    }
                    // Append the permission line and a newline
                    permissionsComponent = permissionsComponent.append(permLine).append(Component.newline());
                }
                // Append the player component and their permissions to hover text
                comp = comp.appendNewline()
                        .append(playerComponent)
                        .append(permissionsComponent);
            }
        }
        comp = comp.append(mm.deserialize(messages.get("region.info.key"), parsed("key", region.getKey().toString())));
        return comp;
    }
}
