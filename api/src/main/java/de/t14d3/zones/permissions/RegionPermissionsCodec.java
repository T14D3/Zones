package de.t14d3.zones.permissions;

import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.subjects.*;

import java.util.*;

/**
 * POJO codec for {@link RegionPermissions}.
 *
 * <p>The in-memory representation stores permission ids for fast lookups, while the persisted representation stores
 * permission names (via {@link PermissionKeyRegistry}) so the data stays stable across restarts.</p>
 */
public final class RegionPermissionsCodec {
    private RegionPermissionsCodec() {
    }

    /**
     * Encodes a {@link RegionPermissions} instance into a serializable structure.
     *
     * <p>Unknown permission ids (missing from {@link PermissionKeyRegistry}) are skipped.</p>
     *
     * @param perms permissions to encode (may be {@code null})
     * @return encoded representation (never {@code null})
     */
    public static Persisted encode(RegionPermissions perms) {
        Persisted persisted = new Persisted();
        if (perms == null) return persisted;

        for (Map.Entry<SubjectRef, RegionPermissions.SubjectPermissions> subjectEntry : perms.subjects().entrySet()) {
            SubjectRef subjectRef = subjectEntry.getKey();
            RegionPermissions.SubjectPermissions subject = subjectEntry.getValue();
            if (subject == null) continue;

            Map<String, Entry> encodedPerms = new HashMap<>();
            for (var permEntry : subject.values().int2ObjectEntrySet()) {
                int permId = permEntry.getIntKey();
                // Persist permission names (stable) rather than numeric ids (runtime-only).
                String permName = PermissionKeyRegistry.instance().getName(permId);
                if (permName == null) continue;

                RegionPermissions.PermissionValue pv = permEntry.getValue();
                if (pv instanceof RegionPermissions.TargetDecisionValue targeted) {
                    Entry e = new Entry();
                    e.kind = "targets";
                    e.allowTargets = new ArrayList<>(targeted.allowTargets());
                    e.denyTargets = new ArrayList<>(targeted.denyTargets());
                    encodedPerms.put(permName, e);
                } else if (pv instanceof RegionPermissions.StringSetValue set) {
                    Entry e = new Entry();
                    e.kind = "set";
                    e.allowValues = new ArrayList<>(set.allowValues());
                    e.denyValues = new ArrayList<>(set.denyValues());
                    encodedPerms.put(permName, e);
                }
            }
            String subjectKey = encodeSubjectKey(subjectRef);
            if (subjectKey != null) persisted.subjects.put(subjectKey, encodedPerms);
        }

        return persisted;
    }

    /**
     * Decodes a persisted representation back into the in-memory format.
     *
     * <p>Missing or invalid entries are ignored.</p>
     *
     * @param persisted persisted representation (may be {@code null})
     * @return decoded permissions (never {@code null})
     */
    public static RegionPermissions decode(Persisted persisted) {
        RegionPermissions perms = new RegionPermissions();
        if (persisted == null || persisted.subjects == null) return perms;

        for (Map.Entry<String, Map<String, Entry>> subjectEntry : persisted.subjects.entrySet()) {
            String subjectKey = subjectEntry.getKey();
            if (subjectKey == null || subjectKey.isBlank()) continue;

            SubjectRef subjectRef = decodeSubjectKey(subjectKey);
            if (subjectRef == null) continue;
            RegionPermissions.SubjectPermissions subject = perms.subject(subjectRef);
            Map<String, Entry> encodedPerms = subjectEntry.getValue();
            if (encodedPerms == null) continue;

            for (Map.Entry<String, Entry> permEntry : encodedPerms.entrySet()) {
                String permName = permEntry.getKey();
                Entry encoded = permEntry.getValue();
                if (permName == null || permName.isBlank() || encoded == null) continue;

                // Ensure the permission name has a numeric id for in-memory storage.
                int permId = PermissionKeyRegistry.instance().getOrCreateId(permName);
                if ("set".equalsIgnoreCase(encoded.kind)) {
                    RegionPermissions.StringSetValue set = (RegionPermissions.StringSetValue) subject.getOrCreate(
                            permId, FlagValueKind.STRING_SET);
                    if (encoded.allowValues != null) encoded.allowValues.forEach(set::allow);
                    if (encoded.denyValues != null) encoded.denyValues.forEach(set::deny);
                } else {
                    RegionPermissions.TargetDecisionValue targeted = (RegionPermissions.TargetDecisionValue) subject.getOrCreate(
                            permId, FlagValueKind.TARGET_DECISION);
                    if (encoded.allowTargets != null) encoded.allowTargets.forEach(targeted::allow);
                    if (encoded.denyTargets != null) encoded.denyTargets.forEach(targeted::deny);
                }
            }
        }
        return perms;
    }

    private static String encodeSubjectKey(SubjectRef subject) {
        if (subject == null) return null;
        if (subject instanceof UniversalSubject) return "universal";
        if (subject instanceof PlayerSubject p) return p.uuid().toString();
        if (subject instanceof GroupSubject g) return "group:" + g.name();
        return null;
    }

    private static SubjectRef decodeSubjectKey(String key) {
        if (key == null || key.isBlank()) return null;
        String k = key.trim();
        if ("universal".equalsIgnoreCase(k)
                || "+universal".equalsIgnoreCase(k)
                || "@all".equalsIgnoreCase(k)
                || "@*".equalsIgnoreCase(k)
                || "+universal".equalsIgnoreCase(k)) {
            return Subjects.universal();
        }
        if (k.regionMatches(true, 0, "group:", 0, "group:".length())) {
            String name = Subjects.normalizeGroupName(k.substring("group:".length()));
            return name == null ? null : Subjects.group(name);
        }
        try {
            return Subjects.player(UUID.fromString(k));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Root persisted representation. Designed to be directly consumable by YAML/JSON serializers.
     */
    public static final class Persisted {
        public Map<String, Map<String, Entry>> subjects = new HashMap<>();
    }

    /**
     * Persisted representation of one permission name.
     *
     * <p>Depending on {@link #kind}, either the {@code *Targets} lists or the {@code *Values} lists are used.</p>
     */
    public static final class Entry {
        public String kind;
        public List<String> allowTargets;
        public List<String> denyTargets;
        public List<String> allowValues;
        public List<String> denyValues;
    }
}
