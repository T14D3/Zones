package de.t14d3.zones.permissions.subjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public final class Subjects {
    private Subjects() {
    }

    public static @NotNull UniversalSubject universal() {
        return UniversalSubject.INSTANCE;
    }

    public static @NotNull PlayerSubject player(@NotNull UUID uuid) {
        return new PlayerSubject(uuid);
    }

    public static @NotNull GroupSubject group(@NotNull String name) {
        return new GroupSubject(name);
    }

    /**
     * Formats a subject for logs/debug output.
     */
    public static @NotNull String format(@Nullable SubjectRef subject) {
        return switch (subject) {
            case null -> "unknown";
            case UniversalSubject u -> "universal";
            case PlayerSubject p -> "player:" + p.uuid();
            case GroupSubject g -> "group:" + g.name();
            default -> subject.kindKey();
        };
    }

    /**
     * Normalizes a group name.
     */
    public static @Nullable String normalizeGroupName(@Nullable String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        return n.isEmpty() ? null : n;
    }
}

