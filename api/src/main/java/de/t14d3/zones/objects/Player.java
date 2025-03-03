package de.t14d3.zones.objects;

import de.t14d3.zones.Zones;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Player implements Audience {
    private final UUID uuid;
    private final String name;
    private Box selection;
    private boolean selectionIsCreating = false;

    protected Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static Player of(UUID uuid, String name) {
        return PlayerRepository.getOrAdd(name, uuid);
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

    public BossBar showBossbar(@NotNull Component component, float percent, BossBar.Color barColor, BossBar.Overlay type) {
        BossBar bar = BossBar.bossBar(component, percent, barColor, type);
        Zones.getInstance().getPlatform().getAudience(this).showBossBar(bar);
        return bar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return uuid.equals(player.uuid);
    }

    public World getWorld() {
        return Zones.getInstance().getPlatform().getWorld(this);
    }

    public BlockLocation getLocation() {
        return Zones.getInstance().getPlatform().getLocation(this);
    }

    public String getMetadata(String key) {
        return Zones.getInstance().getPlatform().getMetadata(this, key);
    }

    public void setMetadata(String key, String value) {
        Zones.getInstance().getPlatform().setMetadata(this, key, value);
    }
}
