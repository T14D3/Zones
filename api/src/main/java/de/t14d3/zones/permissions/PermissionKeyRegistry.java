package de.t14d3.zones.permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime registry mapping permission/flag names to compact int ids for fast lookups.
 *
 * <p>These ids are used for in-memory performance; on-disk formats should still prefer
 * stable names unless an explicit persisted mapping is introduced.</p>
 */
public final class PermissionKeyRegistry {
    private static final PermissionKeyRegistry INSTANCE = new PermissionKeyRegistry();

    public static PermissionKeyRegistry instance() {
        return INSTANCE;
    }

    private final Map<String, Integer> nameToId = new HashMap<>();
    private final List<String> idToName = new ArrayList<>();

    private PermissionKeyRegistry() {
    }

    public synchronized int getOrCreateId(String name) {
        name = normalize(name);
        Integer existing = nameToId.get(name);
        if (existing != null) return existing;
        int id = idToName.size();
        idToName.add(name);
        nameToId.put(name, id);
        return id;
    }

    public synchronized Integer getId(String name) {
        return nameToId.get(normalize(name));
    }

    public synchronized String getName(int id) {
        if (id < 0 || id >= idToName.size()) return null;
        return idToName.get(id);
    }

    private static String normalize(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase();
    }
}

