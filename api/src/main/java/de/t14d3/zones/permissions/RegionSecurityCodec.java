package de.t14d3.zones.permissions;

import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.subjects.*;

import java.util.*;

/**
 * JSON-friendly codec for region security state (ACL + membership).
 *
 * <p>This is primarily used by SQL storage.</p>
 */
public final class RegionSecurityCodec {
    private RegionSecurityCodec() {
    }

    public static Persisted encode(RegionPermissions acl, RegionMembership membership) {
        Persisted out = new Persisted();

        if (acl != null) {
            for (Map.Entry<SubjectRef, RegionPermissions.SubjectPermissions> subjectEntry : acl.subjects().entrySet()) {
                SubjectRef subject = subjectEntry.getKey();
                RegionPermissions.SubjectPermissions perms = subjectEntry.getValue();
                if (subject == null || perms == null) continue;

                Map<String, Entry> encodedPerms = encodeSubjectPermissions(perms);
                if (encodedPerms.isEmpty()) continue;

                if (subject instanceof UniversalSubject) {
                    out.acl.universal = encodedPerms;
                } else if (subject instanceof GroupSubject g) {
                    out.acl.groups.put(g.name(), encodedPerms);
                } else if (subject instanceof PlayerSubject p) {
                    out.acl.players.put(p.uuid().toString(), encodedPerms);
                }
            }
        }

        if (membership != null) {
            for (Map.Entry<UUID, RegionMembership.PlayerEntry> player : membership.players().entrySet()) {
                UUID uuid = player.getKey();
                RegionMembership.PlayerEntry entry = player.getValue();
                if (uuid == null || entry == null) continue;

                Persisted.PlayerMembership pm = new Persisted.PlayerMembership();
                pm.allowRoles = new ArrayList<>(entry.roles().allowValues());
                pm.denyRoles = new ArrayList<>(entry.roles().denyValues());
                pm.allowGroups = new ArrayList<>(entry.groups().allowValues());
                pm.denyGroups = new ArrayList<>(entry.groups().denyValues());

                if (!pm.isEmpty()) out.membership.players.put(uuid.toString(), pm);
            }

            for (Map.Entry<String, RegionMembership.GroupEntry> group : membership.groups().entrySet()) {
                String name = group.getKey();
                RegionMembership.GroupEntry entry = group.getValue();
                if (name == null || entry == null) continue;

                Persisted.GroupMembership gm = new Persisted.GroupMembership();
                gm.allowIncludes = new ArrayList<>(entry.includes().allowValues());
                gm.denyIncludes = new ArrayList<>(entry.includes().denyValues());
                if (!gm.isEmpty()) out.membership.groups.put(name, gm);
            }
        }

        return out;
    }

    public static Decoded decode(Persisted persisted) {
        RegionPermissions acl = new RegionPermissions();
        RegionMembership membership = new RegionMembership();

        if (persisted != null && persisted.acl != null) {
            Persisted.Acl a = persisted.acl;
            if (a.universal != null && !a.universal.isEmpty()) {
                decodeSubjectPermissions(acl.subject(Subjects.universal()), a.universal);
            }
            if (a.players != null) {
                for (Map.Entry<String, Map<String, Entry>> player : a.players.entrySet()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(player.getKey());
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    Map<String, Entry> perms = player.getValue();
                    if (perms == null || perms.isEmpty()) continue;
                    decodeSubjectPermissions(acl.subject(Subjects.player(uuid)), perms);
                }
            }
            if (a.groups != null) {
                for (Map.Entry<String, Map<String, Entry>> group : a.groups.entrySet()) {
                    String name = Subjects.normalizeGroupName(group.getKey());
                    if (name == null) continue;
                    Map<String, Entry> perms = group.getValue();
                    if (perms == null || perms.isEmpty()) continue;
                    decodeSubjectPermissions(acl.subject(Subjects.group(name)), perms);
                }
            }
        }

        if (persisted != null && persisted.membership != null) {
            Persisted.Membership m = persisted.membership;
            if (m.players != null) {
                for (Map.Entry<String, Persisted.PlayerMembership> player : m.players.entrySet()) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(player.getKey());
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    Persisted.PlayerMembership pm = player.getValue();
                    if (pm == null) continue;
                    RegionMembership.PlayerEntry entry = membership.player(uuid);
                    if (pm.allowRoles != null) pm.allowRoles.forEach(entry.roles()::allow);
                    if (pm.denyRoles != null) pm.denyRoles.forEach(entry.roles()::deny);
                    if (pm.allowGroups != null) pm.allowGroups.forEach(entry.groups()::allow);
                    if (pm.denyGroups != null) pm.denyGroups.forEach(entry.groups()::deny);
                }
            }
            if (m.groups != null) {
                for (Map.Entry<String, Persisted.GroupMembership> group : m.groups.entrySet()) {
                    String name = Subjects.normalizeGroupName(group.getKey());
                    if (name == null) continue;
                    Persisted.GroupMembership gm = group.getValue();
                    if (gm == null) continue;
                    RegionMembership.GroupEntry entry = membership.group(name);
                    if (gm.allowIncludes != null) gm.allowIncludes.forEach(entry.includes()::allow);
                    if (gm.denyIncludes != null) gm.denyIncludes.forEach(entry.includes()::deny);
                }
            }
        }

        return new Decoded(acl, membership);
    }

    private static Map<String, Entry> encodeSubjectPermissions(RegionPermissions.SubjectPermissions perms) {
        Map<String, Entry> encodedPerms = new HashMap<>();
        for (var permEntry : perms.values().int2ObjectEntrySet()) {
            int permId = permEntry.getIntKey();
            String permName = PermissionKeyRegistry.instance().getName(permId);
            if (permName == null || permName.isBlank()) continue;

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
        return encodedPerms;
    }

    private static void decodeSubjectPermissions(RegionPermissions.SubjectPermissions into, Map<String, Entry> encodedPerms) {
        if (encodedPerms == null) return;
        for (Map.Entry<String, Entry> permEntry : encodedPerms.entrySet()) {
            String permName = permEntry.getKey();
            Entry encoded = permEntry.getValue();
            if (permName == null || permName.isBlank() || encoded == null) continue;

            int permId = PermissionKeyRegistry.instance().getOrCreateId(permName);
            if ("set".equalsIgnoreCase(encoded.kind)) {
                RegionPermissions.StringSetValue set = (RegionPermissions.StringSetValue) into
                        .getOrCreate(permId, FlagValueKind.STRING_SET);
                if (encoded.allowValues != null) encoded.allowValues.forEach(set::allow);
                if (encoded.denyValues != null) encoded.denyValues.forEach(set::deny);
            } else {
                RegionPermissions.TargetDecisionValue targeted = (RegionPermissions.TargetDecisionValue) into
                        .getOrCreate(permId, FlagValueKind.TARGET_DECISION);
                if (encoded.allowTargets != null) encoded.allowTargets.forEach(targeted::allow);
                if (encoded.denyTargets != null) encoded.denyTargets.forEach(targeted::deny);
            }
        }
    }

    public record Decoded(RegionPermissions acl, RegionMembership membership) {
    }

    public static final class Persisted {
        public Acl acl = new Acl();
        public Membership membership = new Membership();

        public static final class Acl {
            public Map<String, Entry> universal = new HashMap<>();
            public Map<String, Map<String, Entry>> players = new HashMap<>();
            public Map<String, Map<String, Entry>> groups = new HashMap<>();
        }

        public static final class Membership {
            public Map<String, PlayerMembership> players = new HashMap<>();
            public Map<String, GroupMembership> groups = new HashMap<>();
        }

        public static final class PlayerMembership {
            public List<String> allowRoles;
            public List<String> denyRoles;
            public List<String> allowGroups;
            public List<String> denyGroups;

            boolean isEmpty() {
                return (allowRoles == null || allowRoles.isEmpty())
                        && (denyRoles == null || denyRoles.isEmpty())
                        && (allowGroups == null || allowGroups.isEmpty())
                        && (denyGroups == null || denyGroups.isEmpty());
            }
        }

        public static final class GroupMembership {
            public List<String> allowIncludes;
            public List<String> denyIncludes;

            boolean isEmpty() {
                return (allowIncludes == null || allowIncludes.isEmpty())
                        && (denyIncludes == null || denyIncludes.isEmpty());
            }
        }
    }

    public static final class Entry {
        public String kind;
        public List<String> allowTargets;
        public List<String> denyTargets;
        public List<String> allowValues;
        public List<String> denyValues;
    }
}

