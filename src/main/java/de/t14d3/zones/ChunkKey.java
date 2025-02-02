package de.t14d3.zones;

import java.util.Objects;
import java.util.UUID;

public record ChunkKey(int x, int z, UUID world) {

    @Override
    public int hashCode() {
        return Objects.hash(x, z, world);
    }
}
