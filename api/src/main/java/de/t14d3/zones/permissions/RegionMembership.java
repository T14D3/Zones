package de.t14d3.zones.permissions;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Region membership data (roles, groups, and group inheritance).
 *
 * <p>This is separate from the region ACL (see {@link RegionPermissions}) so membership semantics don't leak into
 * permission rules.</p>
 */
public final class RegionMembership {
    private final Map<UUID, PlayerEntry> players = new HashMap<>();
    private final Map<String, GroupEntry> groups = new HashMap<>();

    public Map<UUID, PlayerEntry> players() {
        return players;
    }

    public Map<String, GroupEntry> groups() {
        return groups;
    }

    public @NotNull PlayerEntry player(@NotNull UUID uuid) {
        return players.computeIfAbsent(uuid, ignored -> new PlayerEntry());
    }

    public @NotNull GroupEntry group(@NotNull String name) {
        String key = normalizeName(name);
        return groups.computeIfAbsent(key, ignored -> new GroupEntry());
    }

    public static @Nullable String normalizeName(@Nullable String raw) {
        if (raw == null) return null;
        String n = raw.trim().toLowerCase(Locale.ROOT);
        return n.isEmpty() ? null : n;
    }

    public static final class PlayerEntry {
        private final AllowDenyStringSet roles = new AllowDenyStringSet();
        private final AllowDenyStringSet groups = new AllowDenyStringSet();

        public AllowDenyStringSet roles() {
            return roles;
        }

        public AllowDenyStringSet groups() {
            return groups;
        }
    }

    /**
     * Per-group metadata (currently only inheritance via includes).
     */
    public static final class GroupEntry {
        private final AllowDenyStringSet includes = new AllowDenyStringSet();

        public AllowDenyStringSet includes() {
            return includes;
        }
    }

    /**
     * Simple allow/deny set with normalization and "deny removes allow" behavior.
     */
    public static final class AllowDenyStringSet {
        private final ObjectOpenHashSet<String> allow = new ObjectOpenHashSet<>();
        private final ObjectOpenHashSet<String> deny = new ObjectOpenHashSet<>();

        public Set<String> allowValues() {
            return allow;
        }

        public Set<String> denyValues() {
            return deny;
        }

        public void allow(String value) {
            String v = normalizeName(value);
            if (v == null) return;
            deny.remove(v);
            allow.add(v);
        }

        public void deny(String value) {
            String v = normalizeName(value);
            if (v == null) return;
            allow.remove(v);
            deny.add(v);
        }

        public boolean removeAllow(String value) {
            String v = normalizeName(value);
            if (v == null) return false;
            return allow.remove(v);
        }

        public boolean removeDeny(String value) {
            String v = normalizeName(value);
            if (v == null) return false;
            return deny.remove(v);
        }

        public void clear() {
            allow.clear();
            deny.clear();
        }
    }
}

