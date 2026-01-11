package de.t14d3.zones.visuals.particles;

import de.t14d3.zones.ZonesParticleRole;

/**
 * Maps a visual to platform particle roles.
 */
public final class ParticlePalette {
    public static final ParticlePalette PRIMARY_CORNERS =
            new ParticlePalette(ZonesParticleRole.PRIMARY, ZonesParticleRole.SECONDARY);
    public static final ParticlePalette SECONDARY_CORNERS =
            new ParticlePalette(ZonesParticleRole.SECONDARY, ZonesParticleRole.PRIMARY);
    public static final ParticlePalette PREVIEW =
            new ParticlePalette(ZonesParticleRole.PREVIEW, ZonesParticleRole.PREVIEW);

    private final ZonesParticleRole edgeRole;
    private final ZonesParticleRole cornerRole;

    public ParticlePalette(ZonesParticleRole edgeRole, ZonesParticleRole cornerRole) {
        this.edgeRole = edgeRole == null ? ZonesParticleRole.PRIMARY : edgeRole;
        this.cornerRole = cornerRole == null ? ZonesParticleRole.SECONDARY : cornerRole;
    }

    public ZonesParticleRole edgeRole() {
        return edgeRole;
    }

    public ZonesParticleRole cornerRole() {
        return cornerRole;
    }
}
