package de.t14d3.zones.permissions.subjects;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public record GroupSubject(@NotNull String name) implements SubjectRef {
    public GroupSubject {
        if (name == null) throw new IllegalArgumentException("name");
        name = name.trim().toLowerCase(Locale.ROOT);
        if (name.isEmpty()) throw new IllegalArgumentException("name");
    }

    @Override
    public String kindKey() {
        return "group";
    }
}

