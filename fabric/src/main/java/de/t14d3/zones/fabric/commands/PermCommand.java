package de.t14d3.zones.fabric.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
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
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.players.ProfileResolver;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Fabric implementation of the {@code /zone perm} command tree.
 *
 * <p>Uses explicit subcommands (allow/deny/unset/clear). For target-based flags, use {@code all} as the wildcard
 * pattern (stored internally as {@code *}).</p>
 */
public class PermCommand {
    private final RegionManager regionManager;
    private final MessageFormatService messages;
    private final ZonesFabric mod;

    /**
     * Creates a new permission command tree builder.
     *
     * @param mod owning mod instance
     */
    public PermCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    /**
     * Builds the brigadier command node for {@code /zone perm}.
     *
     * @return command node builder
     */
    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("perm")
                .requires(source -> CommandPermissions.check(source, "zones.set"))
                .then(allow())
                .then(deny())
                .then(unset())
                .then(clear());
    }

    private LiteralArgumentBuilder<CommandSourceStack> allow() {
        return patternCommand("allow", true, false);
    }

    private LiteralArgumentBuilder<CommandSourceStack> deny() {
        return patternCommand("deny", false, false);
    }

    LiteralArgumentBuilder<CommandSourceStack> unset() {
        return patternCommand("unset", true, true);
    }

    LiteralArgumentBuilder<CommandSourceStack> clear() {
        return Commands.literal("clear")
                .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(RootCommand::regionKeySuggestion)
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests(this::suggestTargets)
                                .then(Commands.argument("flag", StringArgumentType.string())
                                        .suggests(this::suggestFlags)
                                        .executes(this::executeClear))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> patternCommand(String name, boolean allow, boolean remove) {
        return Commands.literal(name)
                .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(RootCommand::regionKeySuggestion)
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests(this::suggestTargets)
                                .then(Commands.argument("flag", StringArgumentType.string())
                                        .suggests(this::suggestFlags)
                                        .then(Commands.argument("pattern", StringArgumentType.string())
                                                .suggests(PermCommand::suggestPatterns)
                                                .executes(context -> executePattern(context, allow, remove))))));
    }

    private CompletableFuture<Suggestions> suggestTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Region region = resolveRegion(context);
        if (region == null) return builder.buildFuture();
        if (!canEditRegion(context.getSource(), region)) return builder.buildFuture();

        builder.suggest("+universal", new WrappedComponent(
                messages.component("flags.universal"),
                null,
                null,
                NonWrappingComponentSerializer.INSTANCE
        ));

        regionManager.withWorldReadLock(region.getWorld(), () -> {
            for (String group : region.getGroupNames()) {
                builder.suggest("group:" + group);
            }
        });

        for (RPlayer p : RPlayer.online()) {
            builder.suggest(p.name(), new WrappedComponent(
                    Component.text(p.uuid().toString()),
                    null,
                    null,
                    NonWrappingComponentSerializer.INSTANCE
            ));
        }

        return builder.buildFuture();
    }

    static CompletableFuture<Suggestions> suggestPatterns(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        builder.suggest("*");
        builder.suggest("all");

        String flagName = null;
        try {
            flagName = context.getArgument("flag", String.class);
        } catch (Exception ignored) {
        }
        Flag flag = flagName == null ? null : Flags.getFlag(flagName);
        if (flag != null) {
            FlagValueKind kind = flag.valueKind();
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
                builder.suggest(normalized);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestFlags(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        for (Flag flag : Flags.getFlags()) {
            builder.suggest(flag.name(), new WrappedComponent(
                    messages.component("flags." + flag.name()),
                    null,
                    null,
                    NonWrappingComponentSerializer.INSTANCE
            ));
        }
        return builder.buildFuture();
    }

    int executePattern(CommandContext<CommandSourceStack> context, boolean allow, boolean remove) {
        Region region = resolveRegion(context);
        if (region == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }
        if (!canEditRegion(context.getSource(), region)) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }

        String rawTarget = context.getArgument("target", String.class);
        SubjectRef subject = parseTargetSubject(context.getSource(), rawTarget);
        if (subject == null) {
            context.getSource().sendMessage(messages.component("commands.perm.invalid-target"));
            return 1;
        }

        String flagName = context.getArgument("flag", String.class);
        Flag flag = Flags.getFlag(flagName);
        if (flag == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-permission"));
            return 1;
        }

        if (context.getSource().getPlayer() != null) {
            RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
            if (player != null && !flag.canSet(player, mod.getPlatform())) {
                context.getSource().sendMessage(messages.component("commands.invalid-permission"));
                return 1;
            }
        }

        String rawPattern = context.getArgument("pattern", String.class);

        boolean changed = regionManager.withWorldWriteLock(region.getWorld(), () -> {
            RegionPermissions perms = region.getPermissions();
            RegionMembership membership = region.getMembership();

            boolean didChange;
            if (flag.valueKind() == FlagValueKind.STRING_SET && (flag == Flags.ROLE || flag == Flags.GROUP)) {
                didChange = remove
                        ? applyMembershipUnset(membership, subject, flag, rawPattern)
                        : applyMembership(membership, subject, flag, rawPattern, allow);
            } else {
                if (!remove) {
                    didChange = applyAdd(perms, subject, flag, rawPattern, allow);
                } else {
                    didChange = applyUnset(perms, subject, flag, rawPattern);
                }
                if (didChange) cleanupEmptyValue(perms, subject, flag.id());
            }

            if (!didChange) return false;
            regionManager.saveRegion(region.getKey(), region);
            return true;
        });

        if (!changed) {
            context.getSource().sendMessage(messages.component("commands.perm.nothing-to-remove"));
            return 1;
        }

        mod.getPermissionManager().invalidateAll();

        context.getSource().sendMessage(messages.component(
                remove ? "commands.perm.unset" : (allow ? "commands.perm.allow" : "commands.perm.deny"),
                Placeholders.builder()
                        .string("region", region.getKey().toString())
                        .string("target", rawTarget)
                        .string("permission", flag.name())
                        .string("value", rawPattern)
                        .build()
        ));
        return 1;
    }

    static boolean applyAdd(RegionPermissions perms, SubjectRef subject, Flag flag, String rawPattern, boolean allow) {
        FlagValueKind kind = flag.valueKind();
        if (kind == FlagValueKind.TARGET_DECISION) {
            String pattern = normalizeTargetDecisionValue(rawPattern);
            RegionPermissions.TargetDecisionValue v = (RegionPermissions.TargetDecisionValue) perms
                    .subject(subject)
                    .getOrCreate(flag.id(), FlagValueKind.TARGET_DECISION);
            if (allow) {
                v.allow(pattern);
            } else {
                v.deny(pattern);
            }
            return true;
        }

        if (kind == FlagValueKind.STRING_SET) {
            String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
            if (value.isEmpty()) return false;
            RegionPermissions.StringSetValue v = (RegionPermissions.StringSetValue) perms
                    .subject(subject)
                    .getOrCreate(flag.id(), FlagValueKind.STRING_SET);
            if (allow) v.allow(value);
            else v.deny(value);
            return true;
        }

        return false;
    }

    static boolean applyUnset(RegionPermissions perms, SubjectRef subject, Flag flag, String rawPattern) {
        RegionPermissions.SubjectPermissions subjectPerms = perms.subjects().get(subject);
        if (subjectPerms == null) return false;
        RegionPermissions.PermissionValue pv = subjectPerms.get(flag.id());
        switch (pv) {
            case null -> {
                return false;
            }
            case RegionPermissions.TargetDecisionValue targeted -> {
                String pattern = normalizeTargetDecisionValue(rawPattern);
                return targeted.removeAllow(pattern) || targeted.removeDeny(pattern);
            }
            case RegionPermissions.StringSetValue set -> {
                String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
                if (value.isEmpty()) return false;
                return set.allowValues().remove(value) || set.denyValues().remove(value);
            }
            default -> {
            }
        }

        return false;
    }

    private static boolean applyMembership(RegionMembership membership, SubjectRef subject, Flag flag, String rawPattern, boolean allow) {
        if (membership == null || subject == null || flag == null) return false;
        String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return false;

        if (flag == Flags.ROLE) {
            if (!(subject instanceof PlayerSubject p)) return false;
            RegionMembership.PlayerEntry entry = membership.player(p.uuid());
            if (allow) entry.roles().allow(value);
            else entry.roles().deny(value);
            cleanupEmptyMembership(membership, p.uuid());
            return true;
        }

        if (flag == Flags.GROUP) {
            if (subject instanceof PlayerSubject p) {
                RegionMembership.PlayerEntry entry = membership.player(p.uuid());
                if (allow) entry.groups().allow(value);
                else entry.groups().deny(value);
                cleanupEmptyMembership(membership, p.uuid());
                return true;
            }
            if (subject instanceof GroupSubject g) {
                RegionMembership.GroupEntry entry = membership.group(g.name());
                if (allow) entry.includes().allow(value);
                else entry.includes().deny(value);
                cleanupEmptyGroupDefinition(membership, g.name());
                return true;
            }
            return false;
        }

        return false;
    }

    private static boolean applyMembershipUnset(RegionMembership membership, SubjectRef subject, Flag flag, String rawPattern) {
        if (membership == null || subject == null || flag == null) return false;
        String value = rawPattern == null ? "" : rawPattern.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) return false;

        boolean changed;
        if (flag == Flags.ROLE) {
            if (!(subject instanceof PlayerSubject p)) return false;
            RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
            if (entry == null) return false;
            changed = entry.roles().removeAllow(value) || entry.roles().removeDeny(value);
            if (changed) cleanupEmptyMembership(membership, p.uuid());
            return changed;
        }

        if (flag == Flags.GROUP) {
            if (subject instanceof PlayerSubject p) {
                RegionMembership.PlayerEntry entry = membership.players().get(p.uuid());
                if (entry == null) return false;
                changed = entry.groups().removeAllow(value) || entry.groups().removeDeny(value);
                if (changed) cleanupEmptyMembership(membership, p.uuid());
                return changed;
            }
            if (subject instanceof GroupSubject g) {
                RegionMembership.GroupEntry entry = membership.groups().get(g.name());
                if (entry == null) return false;
                changed = entry.includes().removeAllow(value) || entry.includes().removeDeny(value);
                if (changed) cleanupEmptyGroupDefinition(membership, g.name());
                return changed;
            }
            return false;
        }

        return false;
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

    int executeClear(CommandContext<CommandSourceStack> context) {
        Region region = resolveRegion(context);
        if (region == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }
        if (!canEditRegion(context.getSource(), region)) {
            context.getSource().sendMessage(messages.component("commands.invalid-region"));
            return 1;
        }

        String rawTarget = context.getArgument("target", String.class);
        SubjectRef subject = parseTargetSubject(context.getSource(), rawTarget);
        if (subject == null) {
            context.getSource().sendMessage(messages.component("commands.perm.invalid-target"));
            return 1;
        }

        String flagName = context.getArgument("flag", String.class);
        Flag flag = Flags.getFlag(flagName);
        if (flag == null) {
            context.getSource().sendMessage(messages.component("commands.invalid-permission"));
            return 1;
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
                    didChange = !entry.groups().allowValues().isEmpty() || !entry.groups().denyValues().isEmpty();
                    entry.groups().clear();
                    cleanupEmptyMembership(membership, p.uuid());
                } else if (subject instanceof GroupSubject g) {
                    RegionMembership.GroupEntry entry = membership.groups().get(g.name());
                    if (entry == null) return false;
                    didChange = !entry.includes().allowValues().isEmpty() || !entry.includes().denyValues().isEmpty();
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
            context.getSource().sendMessage(messages.component("commands.perm.nothing-to-remove"));
            return 1;
        }

        mod.getPermissionManager().invalidateAll();

        context.getSource().sendMessage(messages.component(
                "commands.perm.clear",
                Placeholders.builder()
                        .string("region", region.getKey().toString())
                        .string("target", rawTarget)
                        .string("permission", flag.name())
                        .build()
        ));
        return 1;
    }

    private Region resolveRegion(CommandContext<CommandSourceStack> context) {
        String keyRaw;
        try {
            keyRaw = context.getArgument("key", String.class);
        } catch (Exception ignored) {
            return null;
        }
        RegionKey key;
        try {
            key = RegionKey.fromString(keyRaw);
        } catch (Exception ignored) {
            return null;
        }
        return regionManager.regions().get(key.getValue());
    }

    private boolean canEditRegion(CommandSourceStack source, Region region) {
        if (region == null || source == null) return false;
        if (CommandPermissions.check(source, "zones.set.other")) return true;
        if (source.getPlayer() == null) return false;
        return regionManager.withWorldReadLock(region.getWorld(), () -> region.isOwner(source.getPlayer().getUUID()));
    }

    private static SubjectRef parseTargetSubject(CommandSourceStack source, String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) return null;
        String raw = rawTarget.trim();

        if ("+universal".equalsIgnoreCase(raw)
                || "@all".equalsIgnoreCase(raw)
                || "@*".equalsIgnoreCase(raw)) {
            return Subjects.universal();
        }

        if (raw.regionMatches(true, 0, "player:", 0, "player:".length())) {
            String nameOrUuid = raw.substring("player:".length()).trim();
            if (nameOrUuid.isEmpty()) return null;
            try {
                return new PlayerSubject(UUID.fromString(nameOrUuid));
            } catch (IllegalArgumentException ignored) {
            }

            if (source != null) {
                var server = source.getServer();
                var online = server.getPlayerList().getPlayerByName(nameOrUuid);
                if (online != null) return new PlayerSubject(online.getUUID());
                try {
                    ProfileResolver resolver = server.services().profileResolver();
                    GameProfile profile = resolver.fetchByName(raw).orElse(null);
                    if (profile != null) return new PlayerSubject(profile.id());
                } catch (Exception ignored) {
                }
            }
            return null;
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

        if (source != null) {
            var server = source.getServer();
            var online = server.getPlayerList().getPlayerByName(raw);
            if (online != null) return new PlayerSubject(online.getUUID());
            try {
                ProfileResolver resolver = server.services().profileResolver();
                GameProfile profile = resolver.fetchByName(raw).orElse(null);
                if (profile != null) return new PlayerSubject(profile.id());
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static String normalizeTargetDecisionValue(String rawValue) {
        if (rawValue == null) return "";
        String v = rawValue.trim();
        if (v.isEmpty()) return "";
        if ("all".equalsIgnoreCase(v)) return "*";
        return TypeKeys.normalize(v);
    }

    static void cleanupEmptyValue(RegionPermissions perms, SubjectRef subjectRef, int flagId) {
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
}
