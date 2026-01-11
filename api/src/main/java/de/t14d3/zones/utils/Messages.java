package de.t14d3.zones.utils;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.PermissionKeyRegistry;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.subjects.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;

/**
 * Shared message/formatting helpers.
 */
public final class Messages {
    private Messages() {
    }


    /**
     * Formats a region summary for chat output.
     *
     * <p>If {@code showMembers} is enabled, this includes all member subjects and their stored permissions. For
     * performance reasons this method assumes callers already hold any required region/world locks.</p>
     *
     * @param region region to format
     * @param showMembers whether to include member permissions
     * @return formatted component
     */
    public static Component regionInfo(Region region, boolean showMembers) {
        MessageFormatService messages = Rapunzel.context().messages();
        Component comp = messages.component("region.info.details.name",
                Placeholders.builder().string("name", region.getName()).build());

        if (region.getParent() != null) {
            comp = comp.appendNewline().append(messages.component("region.info.details.parent",
                    Placeholders.builder().string("parent", region.getParent().toString()).build()));
        }

        comp = comp.appendNewline().append(messages.component("region.info.details.bounds",
                Placeholders.builder()
                        .string("min", region.getMinString())
                        .string("max", region.getMaxString())
                        .build()));

        comp = comp.appendNewline().append(messages.component("region.info.details.members"));

        if (showMembers) {
            RegionPermissions perms = region.getPermissions();
            RegionMembership membership = region.getMembership();

            LinkedHashMap<SubjectRef, RegionPermissions.SubjectPermissions> subjects = collectSubjects(perms,
                    membership);

            if (subjects.isEmpty()) {
                comp = comp.appendNewline().append(Component.text("  (none)").color(NamedTextColor.GRAY));
            }

            for (Map.Entry<SubjectRef, RegionPermissions.SubjectPermissions> member : subjects.entrySet()) {
                SubjectRef subjectRef = member.getKey();
                RegionPermissions.SubjectPermissions subjectPermissions = member.getValue();
                if (subjectRef == null) continue;

                String subjectName = subjectDisplayName(subjectRef);

                Component subjectComponent = messages.component("region.info.members.name",
                        Placeholders.builder().string("name", subjectName).build()).appendNewline();

                Component membershipComponent = membershipDetails(messages, membership, subjectRef);

                Component permissionsComponent = permissionDetails(messages, subjectPermissions);

                comp = comp.appendNewline().append(subjectComponent).append(membershipComponent)
                        .append(permissionsComponent);
            }
        }
        comp = comp.appendNewline().append(messages.component("region.info.key",
                Placeholders.builder().string("key", region.getKey().toString()).build()));
        return comp;
    }

    private static LinkedHashMap<SubjectRef, RegionPermissions.SubjectPermissions> collectSubjects(
            RegionPermissions perms,
            RegionMembership membership
    ) {
        LinkedHashMap<SubjectRef, RegionPermissions.SubjectPermissions> subjects = new LinkedHashMap<>();
        if (perms == null) return subjects;

        if (membership != null) {
            List<UUID> playerUuids = new ArrayList<>(membership.players().keySet());
            playerUuids.sort(Comparator.comparing(UUID::toString));
            for (UUID uuid : playerUuids) {
                if (uuid == null) continue;
                SubjectRef ref = Subjects.player(uuid);
                subjects.putIfAbsent(ref, perms.subjects().get(ref));
            }

            List<String> groupNames = new ArrayList<>(membership.groups().keySet());
            groupNames.sort(String::compareTo);
            for (String name : groupNames) {
                if (name == null || name.isBlank()) continue;
                SubjectRef ref = Subjects.group(name);
                subjects.putIfAbsent(ref, perms.subjects().get(ref));
            }
        }

        for (Map.Entry<SubjectRef, RegionPermissions.SubjectPermissions> e : perms.subjects().entrySet()) {
            subjects.putIfAbsent(e.getKey(), e.getValue());
        }

        return subjects;
    }

    private static String subjectDisplayName(SubjectRef subjectRef) {
        if (subjectRef instanceof PlayerSubject p) {
            return RPlayer.get(p.uuid()).map(RPlayer::name).orElse(p.uuid().toString());
        }
        if (subjectRef instanceof GroupSubject g) {
            return "group:" + g.name();
        }
        if (subjectRef instanceof UniversalSubject) {
            return "+universal";
        }
        return Subjects.format(subjectRef);
    }

    private static Component membershipDetails(
            MessageFormatService messages,
            RegionMembership membership,
            SubjectRef subjectRef
    ) {
        if (membership == null || subjectRef == null) return Component.empty();

        Component out = Component.empty();
        if (subjectRef instanceof PlayerSubject p) {
            RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
            if (entry == null) return out;

            Component rolesLine = formatAllowDenyLine(messages, "roles", entry.roles());
            if (rolesLine != null) out = out.append(rolesLine).append(Component.newline());

            Component groupsLine = formatAllowDenyLine(messages, "groups", entry.groups());
            if (groupsLine != null) out = out.append(groupsLine).append(Component.newline());
        } else if (subjectRef instanceof GroupSubject g) {
            RegionMembership.GroupEntry entry = membership.groups().get(g.name());
            if (entry == null) return out;

            Component includesLine = formatAllowDenyLine(messages, "includes", entry.includes());
            if (includesLine != null) out = out.append(includesLine).append(Component.newline());
        }

        return out;
    }

    private static Component permissionDetails(
            MessageFormatService messages,
            RegionPermissions.SubjectPermissions subjectPermissions
    ) {
        if (messages == null || subjectPermissions == null) return Component.empty();

        Component out = Component.empty();
        for (var permEntry : subjectPermissions.values().int2ObjectEntrySet()) {
            int permId = permEntry.getIntKey();
            String permName = PermissionKeyRegistry.instance().getName(permId);
            if (permName == null || permName.isBlank()) continue;

            RegionPermissions.PermissionValue value = permEntry.getValue();
            List<Component> formattedComponents = new ArrayList<>();

            if (value instanceof RegionPermissions.TargetDecisionValue targeted) {
                formattedComponents.addAll(
                        formatAllowedDenied(messages, targeted.allowTargets(), targeted.denyTargets()));
            } else if (value instanceof RegionPermissions.StringSetValue set) {
                formattedComponents.addAll(formatAllowedDenied(messages, set.allowValues(), set.denyValues()));
            }

            if (formattedComponents.isEmpty()) continue;

            Component permLine = messages.component(
                    "region.info.members.permission",
                    Placeholders.builder().string("permission", permName).build()
            );
            permLine = appendCommaSeparated(permLine, formattedComponents);
            out = out.append(permLine).append(Component.newline());
        }

        return out;
    }

    private static Component appendCommaSeparated(Component line, List<Component> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line = line.append(Component.text(", ").color(NamedTextColor.GRAY));
            }
            line = line.append(values.get(i));
        }
        return line;
    }

    private static List<Component> formatAllowedDenied(MessageFormatService messages, Iterable<String> allowed, Iterable<String> denied) {
        List<String> allowList = new ArrayList<>();
        for (String v : allowed) {
            if (v != null && !v.isBlank()) allowList.add(v);
        }
        allowList.sort(String::compareTo);

        List<String> denyList = new ArrayList<>();
        for (String v : denied) {
            if (v != null && !v.isBlank()) denyList.add(v);
        }
        denyList.sort(String::compareTo);

        List<Component> formattedComponents = new ArrayList<>(allowList.size() + denyList.size());
        for (String v : allowList) {
            formattedComponents.add(messages.component("region.info.members.values.allowed",
                    Placeholders.builder().string("name", v).build()));
        }
        for (String v : denyList) {
            formattedComponents.add(messages.component("region.info.members.values.denied",
                    Placeholders.builder().string("name", v).build()));
        }
        return formattedComponents;
    }

    private static Component formatAllowDenyLine(MessageFormatService messages, String label, RegionMembership.AllowDenyStringSet set) {
        if (messages == null || label == null || set == null) return null;
        if (set.allowValues().isEmpty() && set.denyValues().isEmpty()) return null;

        List<Component> values = formatAllowedDenied(messages, set.allowValues(), set.denyValues());
        if (values.isEmpty()) return null;

        Component line = messages.component("region.info.members.permission",
                Placeholders.builder().string("permission", label).build());
        return appendCommaSeparated(line, values);
    }
}
