package de.t14d3.zones.utils;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.PermissionKeyRegistry;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.subjects.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            for (Map.Entry<SubjectRef, RegionPermissions.SubjectPermissions> member : perms.subjects().entrySet()) {
                SubjectRef subjectRef = member.getKey();
                RegionPermissions.SubjectPermissions subject = member.getValue();
                if (subjectRef == null || subject == null) continue;

                String subjectName;
                if (subjectRef instanceof PlayerSubject p) {
                    subjectName = RPlayer.get(p.uuid()).map(RPlayer::name).orElse(p.uuid().toString());
                } else if (subjectRef instanceof GroupSubject g) {
                    subjectName = "group:" + g.name();
                } else if (subjectRef instanceof UniversalSubject) {
                    subjectName = "+universal";
                } else {
                    subjectName = Subjects.format(subjectRef);
                }

                Component subjectComponent = messages.component("region.info.members.name",
                        Placeholders.builder().string("name", subjectName).build()).appendNewline();

                Component permissionsComponent = Component.empty();
                for (var permEntry : subject.values().int2ObjectEntrySet()) {
                    int permId = permEntry.getIntKey();
                    String permName = PermissionKeyRegistry.instance().getName(permId);
                    if (permName == null || permName.isBlank()) continue;

                    RegionPermissions.PermissionValue value = permEntry.getValue();
                    List<Component> formattedComponents = new ArrayList<>();

                    if (value instanceof RegionPermissions.TargetDecisionValue targeted) {
                        for (String t : targeted.allowTargets()) {
                            formattedComponents.add(messages.component("region.info.members.values.allowed",
                                    Placeholders.builder().string("value", t).build()));
                        }
                        for (String t : targeted.denyTargets()) {
                            formattedComponents.add(messages.component("region.info.members.values.denied",
                                    Placeholders.builder().string("value", t).build()));
                        }
                    } else if (value instanceof RegionPermissions.StringSetValue set) {
                        for (String s : set.allowValues()) {
                            formattedComponents.add(messages.component("region.info.members.values.allowed",
                                    Placeholders.builder().string("value", s).build()));
                        }
                        for (String s : set.denyValues()) {
                            formattedComponents.add(messages.component("region.info.members.values.denied",
                                    Placeholders.builder().string("value", s).build()));
                        }
                    }

                    if (formattedComponents.isEmpty()) continue;

                    Component permLine = messages.component("region.info.members.permission",
                            Placeholders.builder().string("permission", permName).build());

                    for (int i = 0; i < formattedComponents.size(); i++) {
                        permLine = permLine.append(formattedComponents.get(i));
                        if (i < formattedComponents.size() - 1) {
                            permLine = permLine.append(Component.text(", ").color(NamedTextColor.GRAY));
                        }
                    }
                    permissionsComponent = permissionsComponent.append(permLine).append(Component.newline());
                }

                comp = comp.appendNewline().append(subjectComponent).append(permissionsComponent);
            }
        }
        comp = comp.appendNewline().append(messages.component("region.info.key",
                Placeholders.builder().string("key", region.getKey().toString()).build()));
        return comp;
    }
}
