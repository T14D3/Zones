package de.t14d3.zones.permissions.subjects;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record PlayerSubject(@NotNull UUID uuid) implements SubjectRef {
    public PlayerSubject {
        if (uuid == null) throw new IllegalArgumentException("uuid");
    }

    @Override
    public String kindKey() {
        return "player";
    }
}

