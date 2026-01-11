package de.t14d3.zones.permissions.subjects;

/**
 * Typed subject reference used by region ACLs.
 *
 * <p>Subjects represent "who" a rule applies to (players, groups, universal/non-player).</p>
 */
public sealed interface SubjectRef permits PlayerSubject, GroupSubject, UniversalSubject {
    /**
     * Stable, human-readable kind key for persistence/debugging.
     */
    String kindKey();
}

