package de.t14d3.zones.objects;

import de.t14d3.zones.Zones;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Player implements Audience {
    private final UUID uuid;
    private final String name;
    private Box selection;
    private boolean selectionIsCreating = false;

    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUUID() {
        return uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean hasPermission(String permission) {
        return Zones.getInstance().getPlatform().hasPermission(this, permission);
    }

    public Box getSelection() {
        return selection;
    }

    public void setSelection(Box selection) {
        this.selection = selection;
    }

    public boolean isSelectionCreating() {
        return selectionIsCreating;
    }

    public void setSelectionCreating(boolean selectionIsCreating) {
        this.selectionIsCreating = selectionIsCreating;
    }

    @Override
    public void sendMessage(@NotNull Component component) {
        Zones.getInstance().getPlatform().getAudience(this).sendMessage(component);
    }

    @Override
    public void sendActionBar(@NotNull Component component) {
        Zones.getInstance().getPlatform().getAudience(this).sendActionBar(component);
    }
}
