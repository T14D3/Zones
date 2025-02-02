package de.t14d3.zones;

public final class RegionKey {
    private final int value;

    // Constructs from an 8-character hex string
    public RegionKey(String hex8) {
        if (hex8 == null || hex8.length() != 8) {
            throw new IllegalArgumentException("Input must be exactly 8 hex characters");
        }
        // Validate hex and parse
        try {
            this.value = Integer.parseUnsignedInt(hex8, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input must be a valid 8-character hex string", e);
        }
    }

    public RegionKey(int value) {
        this.value = value;
    }

    // Retrieve the integer value
    public int getValue() {
        return value;
    }

    // Returns the 8-character hex string representation
    @Override
    public String toString() {
        return String.format("%08x", value);
    }

    // Equality and hash code based on the stored value
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof RegionKey other) {
            return this.value == other.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    public static RegionKey fromString(String key) {
        return new RegionKey(key);
    }
}
