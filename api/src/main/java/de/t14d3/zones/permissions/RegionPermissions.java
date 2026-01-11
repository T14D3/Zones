package de.t14d3.zones.permissions;

import de.t14d3.zones.objects.Result;
import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.subjects.SubjectRef;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-region permission overrides grouped by "subject" (player, group, universal, ...).
 *
 * <p>This is intentionally a low-level data container: it only stores explicit allow/deny entries.
 * Any fallback behavior (e.g. per-flag defaults) is handled by the permission evaluation layer.</p>
 *
 * <p>All inputs are normalized (trimmed, lowercased). This class is not thread-safe; callers should
 * use the region/world locks provided by {@code RegionManager} when mutating or iterating.</p>
 */
public final class RegionPermissions {
    private final Map<SubjectRef, SubjectPermissions> subjects = new HashMap<>();

    /**
     * Returns the mutable map of subjects to their stored permission values.
     *
     * <p>The returned map is backed by this instance; changes are reflected immediately.</p>
     */
    public Map<SubjectRef, SubjectPermissions> subjects() {
        return subjects;
    }

    /**
     * Returns (and creates if necessary) the permissions container for a subject.
     *
     * @param subjectKey the subject key (typically a UUID string, a group subject, or a universal subject)
     * @return the subject permissions container
     */
    public SubjectPermissions subject(SubjectRef subject) {
        return subjects.computeIfAbsent(subject, ignored -> new SubjectPermissions());
    }

    /**
     * Permission values for a single subject within a region.
     *
     * <p>Permissions are stored by numeric id (see {@link PermissionKeyRegistry}).</p>
     */
    public static final class SubjectPermissions {
        private final Int2ObjectOpenHashMap<PermissionValue> values = new Int2ObjectOpenHashMap<>();

        /**
         * Returns the stored name for a permission id.
         *
         * @param permissionId numeric permission id
         * @return the stored name, or {@code null} if no name is present
         */
        public PermissionValue get(int permissionId) {
            return values.get(permissionId);
        }

        /**
         * Returns the stored name for a permission id, creating an empty name if missing.
         *
         * @param permissionId numeric permission id
         * @param kind         expected name kind to create when missing
         * @return the existing or newly created name
         */
        public PermissionValue getOrCreate(int permissionId, FlagValueKind kind) {
            PermissionValue existing = values.get(permissionId);
            if (existing != null) return existing;
            PermissionValue created = switch (kind) {
                case TARGET_DECISION -> new TargetDecisionValue();
                case STRING_SET -> new StringSetValue();
            };
            values.put(permissionId, created);
            return created;
        }

        /**
         * Returns the mutable backing map of all values for this subject.
         *
         * <p>This is primarily exposed for persistence/inspection.</p>
         *
         * @return backing map (mutable)
         */
        public Int2ObjectOpenHashMap<PermissionValue> values() {
            return values;
        }
    }

    /**
     * Stored permission name for a flag.
     *
     * <p>Regions store different name shapes depending on the flag's {@link FlagValueKind}.</p>
     */
    public sealed interface PermissionValue permits TargetDecisionValue, StringSetValue {
        /**
         * Returns the {@link FlagValueKind} of this stored name.
         *
         * <p>Callers must ensure that the kind matches the owning flag.</p>
         *
         * @return the name kind
         */
        FlagValueKind kind();
    }

    /**
     * Target-based allow/deny decision name.
     *
     * <p>Stores explicit allow and deny entries for concrete "target keys" (e.g. block ids). Each entry can be
     * either an exact key or a glob pattern containing {@code *}. When evaluating a key:</p>
     *
     * <ul>
     *     <li>Exact deny wins over exact allow</li>
     *     <li>Otherwise the most specific matching glob wins</li>
     *     <li>If allow and deny globs have equal specificity, deny wins</li>
     * </ul>
     */
    public static final class TargetDecisionValue implements PermissionValue {
        /**
         * Bonus added to exact-match specificity so exact keys always outrank wildcard patterns.
         */
        private static final int EXACT_BONUS = 1_000_000;

        private final ObjectOpenHashSet<String> allowTargets = new ObjectOpenHashSet<>();
        private final ObjectOpenHashSet<String> denyTargets = new ObjectOpenHashSet<>();

        private final ConcurrentHashMap<String, Decision> cache = new ConcurrentHashMap<>();
        private volatile GlobPattern[] allowPatterns = new GlobPattern[0];
        private volatile GlobPattern[] denyPatterns = new GlobPattern[0];
        private volatile boolean patternsDirty = true;

        /**
         * {@inheritDoc}
         */
        @Override
        public FlagValueKind kind() {
            return FlagValueKind.TARGET_DECISION;
        }

        /**
         * Adds an allow entry for a target key or glob pattern.
         *
         * <p>If the entry is currently denied, it is removed from the deny set.</p>
         *
         * @param targetKey exact key or glob pattern (supports {@code *})
         */
        public void allow(String targetKey) {
            targetKey = normalize(targetKey);
            if (targetKey == null) return;
            denyTargets.remove(targetKey);
            allowTargets.add(targetKey);
            patternsDirty = true;
            cache.clear();
        }

        /**
         * Adds a deny entry for a target key or glob pattern.
         *
         * <p>If the entry is currently allowed, it is removed from the allow set.</p>
         *
         * @param targetKey exact key or glob pattern (supports {@code *})
         */
        public void deny(String targetKey) {
            targetKey = normalize(targetKey);
            if (targetKey == null) return;
            allowTargets.remove(targetKey);
            denyTargets.add(targetKey);
            patternsDirty = true;
            cache.clear();
        }

        /**
         * Returns all allow entries (exact keys and patterns).
         *
         * @return an unmodifiable set of normalized entries
         */
        public Set<String> allowTargets() {
            return Collections.unmodifiableSet(allowTargets);
        }

        /**
         * Returns all deny entries (exact keys and patterns).
         *
         * @return an unmodifiable set of normalized entries
         */
        public Set<String> denyTargets() {
            return Collections.unmodifiableSet(denyTargets);
        }

        /**
         * Removes an allow entry.
         *
         * @param targetKey exact key or glob pattern (supports {@code *})
         * @return {@code true} if the entry existed and was removed
         */
        public boolean removeAllow(String targetKey) {
            targetKey = normalize(targetKey);
            if (targetKey == null) return false;
            boolean changed = allowTargets.remove(targetKey);
            if (changed) {
                patternsDirty = true;
                cache.clear();
            }
            return changed;
        }

        /**
         * Removes a deny entry.
         *
         * @param targetKey exact key or glob pattern (supports {@code *})
         * @return {@code true} if the entry existed and was removed
         */
        public boolean removeDeny(String targetKey) {
            targetKey = normalize(targetKey);
            if (targetKey == null) return false;
            boolean changed = denyTargets.remove(targetKey);
            if (changed) {
                patternsDirty = true;
                cache.clear();
            }
            return changed;
        }

        /**
         * Removes all allow/deny entries for this name.
         */
        public void clear() {
            allowTargets.clear();
            denyTargets.clear();
            patternsDirty = true;
            cache.clear();
        }

        /**
         * Evaluates this name for a concrete query key.
         *
         * <p>Supports wildcard patterns using '*' (glob, not regex).</p>
         */
        public Decision evaluateMatch(String targetKey) {
            targetKey = normalize(targetKey);
            if (targetKey == null) return Decision.undefined();

            Decision cached = cache.get(targetKey);
            if (cached != null) return cached;

            Decision decided = evaluateMatchNoCache(targetKey);
            cache.put(targetKey, decided);
            return decided;
        }

        /**
         * Convenience wrapper for {@link #evaluateMatch(String)} returning only the {@link Result}.
         *
         * @param targetKey concrete target key
         * @return the evaluated result
         */
        public Result evaluate(String targetKey) {
            return evaluateMatch(targetKey).result();
        }

        Decision evaluateMatchNoCache(String targetKey) {
            // Exact checks always win.
            if (denyTargets.contains(targetKey)) {
                return new Decision(Result.FALSE, EXACT_BONUS + targetKey.length());
            }
            if (allowTargets.contains(targetKey)) {
                return new Decision(Result.TRUE, EXACT_BONUS + targetKey.length());
            }

            rebuildPatternsIfNeeded();

            Decision bestAllow = Decision.undefined();
            for (GlobPattern p : allowPatterns) {
                if (!p.matches(targetKey)) continue;
                int spec = p.specificity();
                if (spec > bestAllow.specificity()) bestAllow = new Decision(Result.TRUE, spec);
            }

            Decision bestDeny = Decision.undefined();
            for (GlobPattern p : denyPatterns) {
                if (!p.matches(targetKey)) continue;
                int spec = p.specificity();
                if (spec > bestDeny.specificity()) bestDeny = new Decision(Result.FALSE, spec);
            }

            if (bestAllow.result() != Result.UNDEFINED && bestDeny.result() != Result.UNDEFINED) {
                if (bestDeny.specificity() > bestAllow.specificity()) return bestDeny;
                if (bestAllow.specificity() > bestDeny.specificity()) return bestAllow;
                // Same specificity: deny wins.
                return bestDeny;
            }
            if (bestDeny.result() != Result.UNDEFINED) return bestDeny;
            if (bestAllow.result() != Result.UNDEFINED) return bestAllow;

            return Decision.undefined();
        }

        void rebuildPatternsIfNeeded() {
            if (!patternsDirty) return;
            synchronized (this) {
                if (!patternsDirty) return;
                // Only entries containing '*' are compiled into glob patterns; exact keys are handled via set lookup.
                allowPatterns = compilePatterns(allowTargets);
                denyPatterns = compilePatterns(denyTargets);
                patternsDirty = false;
            }
        }

        private static GlobPattern[] compilePatterns(Set<String> entries) {
            if (entries == null || entries.isEmpty()) return new GlobPattern[0];
            ArrayList<GlobPattern> out = new ArrayList<>();
            for (String e : entries) {
                if (e == null || !e.contains("*")) continue;
                out.add(GlobPattern.compile(e));
            }
            return out.toArray(new GlobPattern[0]);
        }

        private static String normalize(String targetKey) {
            if (targetKey == null || targetKey.isBlank()) return null;
            return targetKey.trim().toLowerCase();
        }
    }

    /**
     * String set allow/deny name.
     *
     * <p>This is used for flags where the name is a finite set of arbitrary strings.</p>
     */
    public static final class StringSetValue implements PermissionValue {
        private final ObjectOpenHashSet<String> allow = new ObjectOpenHashSet<>();
        private final ObjectOpenHashSet<String> deny = new ObjectOpenHashSet<>();

        /**
         * {@inheritDoc}
         */
        @Override
        public FlagValueKind kind() {
            return FlagValueKind.STRING_SET;
        }

        /**
         * Adds an allowed name to the set.
         *
         * <p>If the name is currently denied, it is removed from the deny set.</p>
         *
         * @param value name to allow (normalized to lower-case)
         */
        public void allow(String value) {
            if (value == null || value.isBlank()) return;
            value = value.trim().toLowerCase();
            deny.remove(value);
            allow.add(value);
        }

        /**
         * Adds a denied name to the set.
         *
         * <p>If the name is currently allowed, it is removed from the allow set.</p>
         *
         * @param value name to deny (normalized to lower-case)
         */
        public void deny(String value) {
            if (value == null || value.isBlank()) return;
            value = value.trim().toLowerCase();
            allow.remove(value);
            deny.add(value);
        }

        /**
         * Returns the allowed values.
         *
         * <p>The returned set is the backing set; callers should treat it as read-only and only insert normalized values.</p>
         *
         * @return backing allow set
         */
        public Set<String> allowValues() {
            return allow;
        }

        /**
         * Returns the denied values.
         *
         * <p>The returned set is the backing set; callers should treat it as read-only and only insert normalized values.</p>
         *
         * @return backing deny set
         */
        public Set<String> denyValues() {
            return deny;
        }

        /**
         * Evaluates the decision for a concrete query name.
         *
         * @param query query name
         * @return {@link Result#TRUE} if allowed, {@link Result#FALSE} if denied, otherwise {@link Result#UNDEFINED}
         */
        public Result evaluate(String query) {
            if (query == null || query.isBlank()) return Result.UNDEFINED;
            query = query.trim().toLowerCase();
            if (deny.contains(query)) return Result.FALSE;
            if (allow.contains(query)) return Result.TRUE;
            return Result.UNDEFINED;
        }
    }
}
