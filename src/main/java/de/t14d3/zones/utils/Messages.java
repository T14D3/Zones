package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Messages {
    private final Zones zones;
    private final Map<String, String> messages = new HashMap<>();

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
        return messages.getOrDefault(key, zones.getConfig().getString("messages.default", key).replaceAll("<key>", key));
    }
}
