package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.commands.utils.CustomArgument;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.permissions.subjects.GroupSubject;
import de.t14d3.zones.permissions.subjects.PlayerSubject;
import de.t14d3.zones.permissions.subjects.SubjectRef;
import de.t14d3.zones.permissions.subjects.Subjects;
import de.t14d3.zones.utils.TypeKeys;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Bukkit implementation of the {@code /zone perm} command tree.
 *
 * <p>Uses explicit subcommands (allow/deny/unset/clear). For target-based flags, the {@code pattern} supports
 * {@code *} wildcards (glob, not regex). For convenience, {@code all} is treated as {@code *}.</p>
 */
public class PermCommand {
    private final RegionManager regionManager;
    private final MessageFormatService messages;
    private final ZonesBukkit plugin;

    /**
     * Creates a new permission command tree builder.
     *
     * @param plugin owning plugin instance
     */
    public PermCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    /**
     * Root {@code /zone perm} command with all subcommands attached.
     */
    public CommandAPICommand perm = new CommandAPICommand("perm")
            .withPermission("zones.set")
            .withSubcommand(allow())
            .withSubcommand(deny())
            .withSubcommand(unset())
            .withSubcommand(clear());

    private CommandAPICommand allow() {
        return new CommandAPICommand("allow")
                .withPermission("zones.set")
                .withArguments(
                        CustomArgument.region("key", "zones.set.other", CustomArgument.MemberType.OWNER),
                        new StringArgument("target").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(() -> suggestTargets(info.sender(),
                                                (Region) info.previousArgs().get("key"))))),
                        new StringArgument("flag").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(this::suggestFlags))),
                        new StringArgument("pattern").replaceSuggestions(ArgumentSuggestions.stringsAsync(info ->
                                CompletableFuture.supplyAsync(
                                        () -> suggestPatterns((String) info.previousArgs().get("flag")))))
                )
                .executes((sender, args) -> {
                    Region region = (Region) args.get("key");
                    if (region == null) {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }

                    String rawTarget = args.getRaw("target");
                    String flagName = (String) args.get("flag");
                    String rawPattern = (String) args.get("pattern");

                    Flag flag = Flags.getFlag(flagName);
                    if (flag == null) {
                        sender.sendMessage(messages.component("commands.invalid-permission"));
                        return;
                    }

                    if (sender instanceof Player bukkitPlayer) {
                        RPlayer rPlayer = RPlayer.wrap(bukkitPlayer).orElse(null);
                        if (rPlayer != null && !flag.canSet(rPlayer, plugin.getPlatform())) {
                            sender.sendMessage(messages.component("commands.invalid-permission"));
                            return;
                        }
                    }

                    SubjectRef subject = parseTargetSubject(rawTarget);
                    if (subject == null) {
                        sender.sendMessage(messages.component("commands.perm.invalid-target"));
                        return;
                    }

                    FlagValueKind kind = flag.valueKind();
                    if (kind != FlagValueKind.TARGET_DECISION && kind != FlagValueKind.STRING_SET) {
                        sender.sendMessage(messages.component("commands.perm.invalid-flag-kind",
                                Placeholders.builder().string("permission", flag.name()).build()));
                        return;
                    }

                    regionManager.withWorldWriteLock(region.getWorld(), () -> {
                        if (kind == FlagValueKind.TARGET_DECISION) {
                            RegionPermissions perms = region.getPermissions();
                            String pattern = normalizeTargetDecisionValue(rawPattern);
                            RegionPermissions.TargetDecisionValue v = (RegionPermissions.TargetDecisionValue) perms
                                    .subject(subject)
                                    .getOrCreate(flag.id(), FlagValueKind.TARGET_DECISION);
                            v.allow(pattern);
                            cleanupEmptyValue(perms, subject, flag.id());
                        } else {
                            String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
                            if (flag == Flags.ROLE) {
                                if (!(subject instanceof PlayerSubject p)) return;
                                RegionMembership membership = region.getMembership();
                                membership.player(p.uuid()).roles().allow(value);
                                cleanupEmptyMembership(membership, p.uuid());
                            } else if (flag == Flags.GROUP) {
                                RegionMembership membership = region.getMembership();
                                if (subject instanceof PlayerSubject p) {
                                    membership.player(p.uuid()).groups().allow(value);
                                    cleanupEmptyMembership(membership, p.uuid());
                                } else if (subject instanceof GroupSubject g) {
                                    membership.group(g.name()).includes().allow(value);
                                    cleanupEmptyGroupDefinition(membership, g.name());
                                } else {
                                    return;
                                }
                            } else {
                                RegionPermissions perms = region.getPermissions();
                                RegionPermissions.StringSetValue v = (RegionPermissions.StringSetValue) perms
                                        .subject(subject)
                                        .getOrCreate(flag.id(), FlagValueKind.STRING_SET);
                                v.allow(value);
                                cleanupEmptyValue(perms, subject, flag.id());
                            }
                        }

                        regionManager.saveRegion(region.getKey(), region);
                    });

                    plugin.getZones().getPermissionManager().invalidateAll();

                    sender.sendMessage(messages.component(
                            "commands.perm.allow",
                            Placeholders.builder()
                                    .string("region", region.getKey().toString())
                                    .string("target", rawTarget)
                                    .string("permission", flag.name())
                                    .string("value", rawPattern)
                                    .build()
                    ));
                });
    }

    private CommandAPICommand deny() {
        return new CommandAPICommand("deny")
                .withPermission("zones.set")
                .withArguments(
                        CustomArgument.region("key", "zones.set.other", CustomArgument.MemberType.OWNER),
                        new StringArgument("target").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(() -> suggestTargets(info.sender(),
                                                (Region) info.previousArgs().get("key"))))),
                        new StringArgument("flag").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(this::suggestFlags))),
                        new StringArgument("pattern").replaceSuggestions(ArgumentSuggestions.stringsAsync(info ->
                                CompletableFuture.supplyAsync(
                                        () -> suggestPatterns((String) info.previousArgs().get("flag")))))
                )
                .executes((sender, args) -> {
                    Region region = (Region) args.get("key");
                    if (region == null) {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }

                    String rawTarget = args.getRaw("target");
                    String flagName = (String) args.get("flag");
                    String rawPattern = (String) args.get("pattern");

                    Flag flag = Flags.getFlag(flagName);
                    if (flag == null) {
                        sender.sendMessage(messages.component("commands.invalid-permission"));
                        return;
                    }

                    if (sender instanceof Player bukkitPlayer) {
                        RPlayer rPlayer = RPlayer.wrap(bukkitPlayer).orElse(null);
                        if (rPlayer != null && !flag.canSet(rPlayer, plugin.getPlatform())) {
                            sender.sendMessage(messages.component("commands.invalid-permission"));
                            return;
                        }
                    }

                    SubjectRef subject = parseTargetSubject(rawTarget);
                    if (subject == null) {
                        sender.sendMessage(messages.component("commands.perm.invalid-target"));
                        return;
                    }

                    FlagValueKind kind = flag.valueKind();
                    if (kind != FlagValueKind.TARGET_DECISION && kind != FlagValueKind.STRING_SET) {
                        sender.sendMessage(messages.component("commands.perm.invalid-flag-kind",
                                Placeholders.builder().string("permission", flag.name()).build()));
                        return;
                    }

                    regionManager.withWorldWriteLock(region.getWorld(), () -> {
                        if (kind == FlagValueKind.TARGET_DECISION) {
                            RegionPermissions perms = region.getPermissions();
                            String pattern = normalizeTargetDecisionValue(rawPattern);
                            RegionPermissions.TargetDecisionValue v = (RegionPermissions.TargetDecisionValue) perms
                                    .subject(subject)
                                    .getOrCreate(flag.id(), FlagValueKind.TARGET_DECISION);
                            v.deny(pattern);
                            cleanupEmptyValue(perms, subject, flag.id());
                        } else {
                            String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
                            if (flag == Flags.ROLE) {
                                if (!(subject instanceof PlayerSubject p)) return;
                                RegionMembership membership = region.getMembership();
                                membership.player(p.uuid()).roles().deny(value);
                                cleanupEmptyMembership(membership, p.uuid());
                            } else if (flag == Flags.GROUP) {
                                RegionMembership membership = region.getMembership();
                                if (subject instanceof PlayerSubject p) {
                                    membership.player(p.uuid()).groups().deny(value);
                                    cleanupEmptyMembership(membership, p.uuid());
                                } else if (subject instanceof GroupSubject g) {
                                    membership.group(g.name()).includes().deny(value);
                                    cleanupEmptyGroupDefinition(membership, g.name());
                                } else {
                                    return;
                                }
                            } else {
                                RegionPermissions perms = region.getPermissions();
                                RegionPermissions.StringSetValue v = (RegionPermissions.StringSetValue) perms
                                        .subject(subject)
                                        .getOrCreate(flag.id(), FlagValueKind.STRING_SET);
                                v.deny(value);
                                cleanupEmptyValue(perms, subject, flag.id());
                            }
                        }

                        regionManager.saveRegion(region.getKey(), region);
                    });

                    plugin.getZones().getPermissionManager().invalidateAll();

                    sender.sendMessage(messages.component(
                            "commands.perm.deny",
                            Placeholders.builder()
                                    .string("region", region.getKey().toString())
                                    .string("target", rawTarget)
                                    .string("permission", flag.name())
                                    .string("value", rawPattern)
                                    .build()
                    ));
                });
    }

    CommandAPICommand unset() {
        return new CommandAPICommand("unset")
                .withPermission("zones.set")
                .withArguments(
                        CustomArgument.region("key", "zones.set.other", CustomArgument.MemberType.OWNER),
                        new StringArgument("target").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(() -> suggestTargets(info.sender(),
                                                (Region) info.previousArgs().get("key"))))),
                        new StringArgument("flag").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(this::suggestFlags))),
                        new StringArgument("pattern").replaceSuggestions(ArgumentSuggestions.stringsAsync(info ->
                                CompletableFuture.supplyAsync(
                                        () -> suggestPatterns((String) info.previousArgs().get("flag")))))
                )
                .executes((sender, args) -> {
                    Region region = (Region) args.get("key");
                    if (region == null) {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }

                    String rawTarget = args.getRaw("target");
                    String flagName = (String) args.get("flag");
                    String rawPattern = (String) args.get("pattern");

                    Flag flag = Flags.getFlag(flagName);
                    if (flag == null) {
                        sender.sendMessage(messages.component("commands.invalid-permission"));
                        return;
                    }

                    if (sender instanceof Player bukkitPlayer) {
                        RPlayer rPlayer = RPlayer.wrap(bukkitPlayer).orElse(null);
                        if (rPlayer != null && !flag.canSet(rPlayer, plugin.getPlatform())) {
                            sender.sendMessage(messages.component("commands.invalid-permission"));
                            return;
                        }
                    }

                    SubjectRef subject = parseTargetSubject(rawTarget);
                    if (subject == null) {
                        sender.sendMessage(messages.component("commands.perm.invalid-target"));
                        return;
                    }

                    boolean changed = regionManager.withWorldWriteLock(region.getWorld(), () -> {
                        FlagValueKind kind = flag.valueKind();
                        boolean didChange = false;

                        if (kind == FlagValueKind.TARGET_DECISION) {
                            RegionPermissions perms = region.getPermissions();
                            RegionPermissions.SubjectPermissions subjectPerms = perms.subjects().get(subject);
                            if (subjectPerms == null) return false;
                            RegionPermissions.PermissionValue pv = subjectPerms.get(flag.id());
                            if (!(pv instanceof RegionPermissions.TargetDecisionValue targeted)) return false;
                            String pattern = normalizeTargetDecisionValue(rawPattern);
                            didChange = targeted.removeAllow(pattern) || targeted.removeDeny(pattern);
                            if (didChange) cleanupEmptyValue(perms, subject, flag.id());
                        } else if (kind == FlagValueKind.STRING_SET) {
                            String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
                            if (value.isEmpty()) return false;
                            RegionMembership membership = region.getMembership();

                            if (flag == Flags.ROLE) {
                                if (!(subject instanceof PlayerSubject p)) return false;
                                RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
                                if (entry == null) return false;
                                didChange = entry.roles().removeAllow(value) || entry.roles().removeDeny(value);
                                if (didChange) cleanupEmptyMembership(membership, p.uuid());
                            } else if (flag == Flags.GROUP) {
                                if (subject instanceof PlayerSubject p) {
                                    RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
                                    if (entry == null) return false;
                                    didChange = entry.groups().removeAllow(value) || entry.groups().removeDeny(value);
                                    if (didChange) cleanupEmptyMembership(membership, p.uuid());
                                } else if (subject instanceof GroupSubject g) {
                                    RegionMembership.GroupEntry entry = membership.groups().get(g.name());
                                    if (entry == null) return false;
                                    didChange = entry.includes().removeAllow(value) || entry.includes()
                                            .removeDeny(value);
                                    if (didChange) cleanupEmptyGroupDefinition(membership, g.name());
                                } else {
                                    return false;
                                }
                            } else {
                                RegionPermissions perms = region.getPermissions();
                                RegionPermissions.SubjectPermissions subjectPerms = perms.subjects().get(subject);
                                if (subjectPerms == null) return false;
                                RegionPermissions.PermissionValue pv = subjectPerms.get(flag.id());
                                if (!(pv instanceof RegionPermissions.StringSetValue set)) return false;
                                didChange = set.allowValues().remove(value) || set.denyValues().remove(value);
                                if (didChange) cleanupEmptyValue(perms, subject, flag.id());
                            }
                        } else {
                            return false;
                        }

                        if (!didChange) return false;
                        regionManager.saveRegion(region.getKey(), region);
                        return true;
                    });

                    if (!changed) {
                        sender.sendMessage(messages.component("commands.perm.nothing-to-remove"));
                        return;
                    }

                    plugin.getZones().getPermissionManager().invalidateAll();

                    sender.sendMessage(messages.component(
                            "commands.perm.unset",
                            Placeholders.builder()
                                    .string("region", region.getKey().toString())
                                    .string("target", rawTarget)
                                    .string("permission", flag.name())
                                    .string("value", rawPattern)
                                    .build()
                    ));
                });
    }

    CommandAPICommand clear() {
        return new CommandAPICommand("clear")
                .withPermission("zones.set")
                .withArguments(
                        CustomArgument.region("key", "zones.set.other", CustomArgument.MemberType.OWNER),
                        new StringArgument("target").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(() -> suggestTargets(info.sender(),
                                                (Region) info.previousArgs().get("key"))))),
                        new StringArgument("flag").replaceSuggestions(
                                ArgumentSuggestions.stringsWithTooltipsAsync(info ->
                                        CompletableFuture.supplyAsync(this::suggestFlags)))
                )
                .executes((sender, args) -> {
                    Region region = (Region) args.get("key");
                    if (region == null) {
                        sender.sendMessage(messages.component("commands.invalid-region"));
                        return;
                    }

                    String rawTarget = args.getRaw("target");
                    String flagName = (String) args.get("flag");

                    Flag flag = Flags.getFlag(flagName);
                    if (flag == null) {
                        sender.sendMessage(messages.component("commands.invalid-permission"));
                        return;
                    }

                    if (sender instanceof Player bukkitPlayer) {
                        RPlayer rPlayer = RPlayer.wrap(bukkitPlayer).orElse(null);
                        if (rPlayer != null && !flag.canSet(rPlayer, plugin.getPlatform())) {
                            sender.sendMessage(messages.component("commands.invalid-permission"));
                            return;
                        }
                    }

                    SubjectRef subject = parseTargetSubject(rawTarget);
                    if (subject == null) {
                        sender.sendMessage(messages.component("commands.perm.invalid-target"));
                        return;
                    }

                    boolean changed = regionManager.withWorldWriteLock(region.getWorld(), () -> {
                        FlagValueKind kind = flag.valueKind();
                        boolean didChange = false;

                        if (kind == FlagValueKind.STRING_SET && flag == Flags.ROLE) {
                            if (!(subject instanceof PlayerSubject p)) return false;
                            RegionMembership membership = region.getMembership();
                            RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
                            if (entry == null) return false;
                            didChange = !entry.roles().allowValues().isEmpty() || !entry.roles().denyValues().isEmpty();
                            entry.roles().clear();
                            cleanupEmptyMembership(membership, p.uuid());
                        } else if (kind == FlagValueKind.STRING_SET && flag == Flags.GROUP) {
                            RegionMembership membership = region.getMembership();
                            if (subject instanceof PlayerSubject p) {
                                RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
                                if (entry == null) return false;
                                didChange = !entry.groups().allowValues().isEmpty() || !entry.groups().denyValues()
                                        .isEmpty();
                                entry.groups().clear();
                                cleanupEmptyMembership(membership, p.uuid());
                            } else if (subject instanceof GroupSubject g) {
                                RegionMembership.GroupEntry entry = membership.groups().get(g.name());
                                if (entry == null) return false;
                                didChange = !entry.includes().allowValues().isEmpty() || !entry.includes().denyValues()
                                        .isEmpty();
                                entry.includes().clear();
                                cleanupEmptyGroupDefinition(membership, g.name());
                            } else {
                                return false;
                            }
                        } else if (kind == FlagValueKind.TARGET_DECISION || kind == FlagValueKind.STRING_SET) {
                            RegionPermissions perms = region.getPermissions();
                            RegionPermissions.SubjectPermissions subjectPerms = perms.subjects().get(subject);
                            if (subjectPerms == null || subjectPerms.get(flag.id()) == null) return false;
                            subjectPerms.values().remove(flag.id());
                            if (subjectPerms.values().isEmpty()) perms.subjects().remove(subject);
                            didChange = true;
                        } else {
                            return false;
                        }

                        if (!didChange) return false;
                        regionManager.saveRegion(region.getKey(), region);
                        return true;
                    });

                    if (!changed) {
                        sender.sendMessage(messages.component("commands.perm.nothing-to-remove"));
                        return;
                    }

                    plugin.getZones().getPermissionManager().invalidateAll();

                    sender.sendMessage(messages.component(
                            "commands.perm.clear",
                            Placeholders.builder()
                                    .string("region", region.getKey().toString())
                                    .string("target", rawTarget)
                                    .string("permission", flag.name())
                                    .build()
                    ));
                });
    }


    private static void cleanupEmptyValue(RegionPermissions perms, SubjectRef subjectRef, int flagId) {
        if (perms == null || subjectRef == null) return;
        RegionPermissions.SubjectPermissions subject = perms.subjects().get(subjectRef);
        if (subject == null) return;

        RegionPermissions.PermissionValue pv = subject.get(flagId);
        if (pv instanceof RegionPermissions.TargetDecisionValue targeted) {
            if (targeted.allowTargets().isEmpty() && targeted.denyTargets().isEmpty()) {
                subject.values().remove(flagId);
            }
        } else if (pv instanceof RegionPermissions.StringSetValue set) {
            if (set.allowValues().isEmpty() && set.denyValues().isEmpty()) {
                subject.values().remove(flagId);
            }
        }

        if (subject.values().isEmpty()) perms.subjects().remove(subjectRef);
    }

    private static void cleanupEmptyMembership(RegionMembership membership, UUID uuid) {
        if (membership == null || uuid == null) return;
        RegionMembership.PlayerEntry entry = membership.players().get(uuid);
        if (entry == null) return;

        boolean rolesEmpty = entry.roles().allowValues().isEmpty() && entry.roles().denyValues().isEmpty();
        boolean groupsEmpty = entry.groups().allowValues().isEmpty() && entry.groups().denyValues().isEmpty();
        if (rolesEmpty && groupsEmpty) membership.players().remove(uuid);
    }

    private static void cleanupEmptyGroupDefinition(RegionMembership membership, String groupName) {
        if (membership == null || groupName == null) return;
        RegionMembership.GroupEntry entry = membership.groups().get(groupName);
        if (entry == null) return;
        if (entry.includes().allowValues().isEmpty() && entry.includes().denyValues().isEmpty()) {
            membership.groups().remove(groupName);
        }
    }

    private StringTooltip[] suggestTargets(CommandSender sender, Region region) {
        if (region == null) return new StringTooltip[0];

        boolean canEdit = sender.hasPermission("zones.set.other");
        if (!canEdit && sender instanceof Player p) {
            canEdit = regionManager.withWorldReadLock(region.getWorld(), () -> region.isOwner(p.getUniqueId()));
        }
        if (!canEdit) return new StringTooltip[0];

        Map<String, Component> targets = new HashMap<>();
        targets.put("+universal", messages.component("flags.universal"));

        regionManager.withWorldReadLock(region.getWorld(), () -> {
            for (String group : region.getGroupNames()) {
                targets.put("group:" + group, Component.text(region.getGroupMembers(group).toString()));
            }
        });

        for (var player : RPlayer.online()) {
            targets.put(player.name(), Component.text(player.uuid().toString()));
        }

        StringTooltip[] suggestions = new StringTooltip[targets.size()];
        int i = 0;
        for (Map.Entry<String, Component> target : targets.entrySet()) {
            suggestions[i++] = StringTooltip.ofMessage(
                    target.getKey(),
                    BukkitTooltip.messageFromAdventureComponent(target.getValue())
            );
        }
        return suggestions;
    }

    private StringTooltip[] suggestFlags() {
        List<StringTooltip> out = new ArrayList<>();
        for (Flag flag : Flags.getFlags()) {
            out.add(StringTooltip.ofMessage(
                    flag.name(),
                    BukkitTooltip.messageFromAdventureComponent(messages.component("flags." + flag.name()))
            ));
        }
        return out.toArray(new StringTooltip[0]);
    }


    private static String[] suggestPatterns(String flagName) {
        if (flagName == null) return new String[]{"all"};
        Flag flag = Flags.getFlag(flagName);
        if (flag == null) return new String[]{"all"};

        FlagValueKind kind = flag.valueKind();
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("*");
        values.add("all");
        for (String v : flag.getValidValues()) {
            if (v == null || v.isBlank()) continue;
            String normalized = v.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("!")) continue;
            if (kind == FlagValueKind.TARGET_DECISION
                    && ("true".equals(normalized)
                    || "false".equals(normalized)
                    || "owner".equals(normalized)
                    || "admin".equals(normalized))) {
                continue;
            }
            values.add(normalized);
        }
        return values.toArray(new String[0]);
    }

    private static SubjectRef parseTargetSubject(String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) return null;
        String raw = rawTarget.trim();

        if ("+universal".equalsIgnoreCase(raw)) {
            return Subjects.universal();
        }

        if (raw.regionMatches(true, 0, "player:", 0, "player:".length())) {
            String nameOrUuid = raw.substring("player:".length()).trim();
            if (nameOrUuid.isEmpty()) return null;
            try {
                return new PlayerSubject(UUID.fromString(nameOrUuid));
            } catch (IllegalArgumentException ignored) {
                return new PlayerSubject(Bukkit.getOfflinePlayer(nameOrUuid).getUniqueId());
            }
        }

        if (raw.regionMatches(true, 0, "group:", 0, "group:".length())) {
            String name = Subjects.normalizeGroupName(raw.substring("group:".length()));
            return name == null ? null : new GroupSubject(name);
        }

        if (raw.regionMatches(true, 0, "group.", 0, "group.".length())) {
            String name = Subjects.normalizeGroupName(raw.substring("group.".length()));
            return name == null ? null : new GroupSubject(name);
        }

        try {
            return new PlayerSubject(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
        }

        return new PlayerSubject(Bukkit.getOfflinePlayer(raw).getUniqueId());
    }

    private static String normalizeTargetDecisionValue(String rawValue) {
        if (rawValue == null) return "";
        String v = rawValue.trim();
        if (v.isEmpty()) return "";
        if ("all".equalsIgnoreCase(v)) return "*";
        return TypeKeys.normalize(v);
    }
}
