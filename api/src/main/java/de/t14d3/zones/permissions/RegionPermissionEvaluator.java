package de.t14d3.zones.permissions;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.objects.Result;
import de.t14d3.zones.permissions.subjects.GroupSubject;
import de.t14d3.zones.permissions.subjects.PlayerSubject;
import de.t14d3.zones.permissions.subjects.SubjectRef;
import de.t14d3.zones.permissions.subjects.Subjects;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

final class RegionPermissionEvaluator {
    private final RegionManager regionManager;
    private final LongSupplier versionSupplier;
    private final long ttlMillis;
    private final int limit;

    private final IdRegistry<SubjectRef> subjectIds = new IdRegistry<>();
    private final StringIdRegistry typeIds = new StringIdRegistry();

    private final ConcurrentHashMap<EvalKey, CachedDecision> decisionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, CachedMemberContext> memberCache = new ConcurrentHashMap<>();

    RegionPermissionEvaluator(
            RegionManager regionManager,
            LongSupplier versionSupplier,
            long ttlMillis,
            int limit
    ) {
        this.regionManager = regionManager;
        this.versionSupplier = versionSupplier;
        this.ttlMillis = ttlMillis;
        this.limit = limit;
    }

    Decision evaluate(Region region, SubjectRef subject, Flag flag, String typeKey) {
        if (region == null || flag == null) return Decision.undefined();

        String type = typeKey == null ? "" : typeKey;

        long now = System.currentTimeMillis();
        long version = versionSupplier.getAsLong();

        int regionId = region.getKey().getValue();
        int subjectId = subjectIds.id(subject != null ? subject : Subjects.universal());
        int typeId = typeIds.id(type);

        EvalKey key = new EvalKey(pack(regionId, subjectId), pack(flag.id(), typeId));
        CachedDecision cached = decisionCache.get(key);
        if (cached != null && cached.version == version && cached.expiresAtMillis > now) {
            return cached.decision;
        }

        // Evaluation order:
        // 1) Explicit subject rules (including targeted rules by type)
        // 2) Admin/owner role-based bypass for player subjects
        // 3) Group subjects (best specificity wins; deny wins ties)
        // 4) Universal subject

        Decision direct = decisionForSubject(region, subject, flag, type);
        Decision computed;
        if (direct.result() != Result.UNDEFINED) {
            computed = direct;
        } else if (subject instanceof PlayerSubject && memberContext(region, subject).isAdmin) {
            computed = new Decision(Result.TRUE, Integer.MAX_VALUE);
        } else {
            CachedMemberContext ctx = memberContext(region, subject);
            Decision bestGroup = Decision.undefined();
            for (String group : ctx.groups) {
                Decision d = decisionForSubject(region, new GroupSubject(group), flag, type);
                if (d.result() == Result.UNDEFINED) continue;
                if (bestGroup.result() == Result.UNDEFINED
                        || d.specificity() > bestGroup.specificity()
                        || (d.specificity() == bestGroup.specificity() && d.result() == Result.FALSE)) {
                    bestGroup = d;
                }
            }

            if (bestGroup.result() != Result.UNDEFINED) {
                computed = bestGroup;
            } else {
                Decision universal = decisionForSubject(region, Subjects.universal(), flag, type);
                computed = (universal.result() != Result.UNDEFINED) ? universal : Decision.undefined();
            }
        }

        if (limit > 0 && decisionCache.size() > limit) {
            decisionCache.clear();
        }
        decisionCache.put(key, new CachedDecision(computed, version, now + ttlMillis));
        return computed;
    }

    void invalidateAll() {
        decisionCache.clear();
        memberCache.clear();
    }

    void invalidateSubject(SubjectRef subject) {
        if (subject == null) return;
        Integer subjectId = subjectIds.findId(subject);
        if (subjectId == null) return;

        // Remove member contexts.
        memberCache.keySet().removeIf(k -> (int) (k & 0xFFFFFFFFL) == subjectId);

        // Remove cached decisions.
        decisionCache.keySet().removeIf(k -> (int) (k.regionAndSubject & 0xFFFFFFFFL) == subjectId);
    }

    private CachedMemberContext memberContext(Region region, SubjectRef subject) {
        long now = System.currentTimeMillis();
        long version = versionSupplier.getAsLong();

        int regionId = region.getKey().getValue();
        int subjectId = subjectIds.id(subject);
        long key = pack(regionId, subjectId);

        CachedMemberContext cached = memberCache.get(key);
        if (cached != null && cached.version == version && cached.expiresAtMillis > now) {
            return cached;
        }

        // Only player subjects have membership context.
        if (!(subject instanceof PlayerSubject player)) {
            CachedMemberContext computed = new CachedMemberContext(false, new String[0], version, now + ttlMillis);
            memberCache.put(key, computed);
            return computed;
        }

        // Build lineage root->leaf to apply parent roles/groups first.
        ArrayDeque<Region> lineage = new ArrayDeque<>();
        Region current = region;
        Set<Integer> visited = null;
        while (current != null) {
            lineage.addFirst(current);
            if (current.getParent() == null) break;
            if (visited == null) visited = new HashSet<>();
            if (!visited.add(current.getKey().getValue())) break;
            current = current.getParentRegion(regionManager);
        }

        Set<String> roles = new HashSet<>();
        Set<String> directGroups = new HashSet<>();
        Map<String, Set<String>> includes = new HashMap<>();

        for (Region r : lineage) {
            RegionMembership.PlayerEntry entry = r.getMembership().players().get(player.uuid());
            if (entry != null) {
                for (String deny : entry.roles().denyValues()) roles.remove(deny);
                for (String allow : entry.roles().allowValues()) roles.add(allow);
                for (String deny : entry.groups().denyValues()) directGroups.remove(deny);
                for (String allow : entry.groups().allowValues()) directGroups.add(allow);
            }

            for (Map.Entry<String, RegionMembership.GroupEntry> ge : r.getMembership().groups().entrySet()) {
                String groupName = ge.getKey();
                RegionMembership.GroupEntry groupEntry = ge.getValue();
                if (groupName == null || groupEntry == null) continue;
                Set<String> set = includes.computeIfAbsent(groupName, ignored -> new HashSet<>());
                for (String deny : groupEntry.includes().denyValues()) set.remove(deny);
                for (String allow : groupEntry.includes().allowValues()) set.add(allow);
            }
        }

        boolean isAdmin = roles.contains("owner") || roles.contains("admin");

        Set<String> expandedGroups = new HashSet<>(directGroups);
        ArrayDeque<String> queue = new ArrayDeque<>(directGroups);
        while (!queue.isEmpty()) {
            String g = queue.removeFirst();
            Set<String> inc = includes.get(g);
            if (inc == null) continue;
            for (String child : inc) {
                if (child == null || child.isBlank()) continue;
                if (expandedGroups.add(child)) queue.addLast(child);
            }
        }

        CachedMemberContext computed = new CachedMemberContext(isAdmin, expandedGroups.toArray(new String[0]), version,
                now + ttlMillis);
        if (limit > 0 && memberCache.size() > limit) {
            memberCache.clear();
        }
        memberCache.put(key, computed);
        return computed;
    }

    private Decision decisionForSubject(Region region, SubjectRef subject, Flag flag, String typeKey) {
        Region current = region;
        Set<Integer> visited = null;
        while (current != null) {
            RegionPermissions perms = current.getPermissions();
            RegionPermissions.SubjectPermissions subjectPerms = perms.subjects().get(subject);
            if (subjectPerms != null) {
                RegionPermissions.PermissionValue pv = subjectPerms.get(flag.id());
                if (pv instanceof RegionPermissions.TargetDecisionValue targeted) {
                    Decision d = targeted.evaluateMatch(typeKey);
                    if (d.result() != Result.UNDEFINED) return d;
                } else if (pv instanceof RegionPermissions.StringSetValue set) {
                    Result r = set.evaluate(typeKey);
                    if (r != Result.UNDEFINED) return new Decision(r, Integer.MAX_VALUE - 1);
                }
            }

            if (current.getParent() == null) break;
            if (visited == null) visited = new HashSet<>();
            if (!visited.add(current.getKey().getValue())) break;
            current = current.getParentRegion(regionManager);
        }
        return Decision.undefined();
    }

    private static long pack(int high, int low) {
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }

    private static final class IdRegistry<T> {
        private final ConcurrentHashMap<T, Integer> ids = new ConcurrentHashMap<>();
        private final AtomicInteger next = new AtomicInteger(1);

        int id(T key) {
            if (key == null) return 0;
            T k = key;
            return ids.computeIfAbsent(k, ignored -> next.getAndIncrement());
        }

        Integer findId(T key) {
            if (key == null) return null;
            return ids.get(key);
        }
    }

    private static final class StringIdRegistry {
        private final ConcurrentHashMap<String, Integer> ids = new ConcurrentHashMap<>();
        private final AtomicInteger next = new AtomicInteger(1);

        int id(String key) {
            if (key == null) key = "";
            String k = key;
            return ids.computeIfAbsent(k, ignored -> next.getAndIncrement());
        }

        Integer findId(String key) {
            if (key == null) return null;
            return ids.get(key);
        }
    }

    private record EvalKey(long regionAndSubject, long flagAndType) {
    }

    private record CachedDecision(Decision decision, long version, long expiresAtMillis) {
    }

    private record CachedMemberContext(boolean isAdmin, String[] groups, long version, long expiresAtMillis) {
    }
}
