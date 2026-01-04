package de.t14d3.zones.permissions.subjects;

/**
 * Universal/non-player subject singleton.
 */
public final class UniversalSubject implements SubjectRef {
    public static final UniversalSubject INSTANCE = new UniversalSubject();

    private UniversalSubject() {
    }

    @Override
    public String kindKey() {
        return "universal";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof UniversalSubject;
    }

    @Override
    public int hashCode() {
        return 31_337;
    }

    @Override
    public String toString() {
        return "universal";
    }
}

