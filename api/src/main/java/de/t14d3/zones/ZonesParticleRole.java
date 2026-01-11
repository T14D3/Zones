package de.t14d3.zones;

/**
 * Semantic particle channel used by {@link ZonesPlatform}.
 *
 * <p>Platform implementations map these roles to concrete particle effects (configured as
 * {@code visuals.particles.primary}/{@code visuals.particles.secondary}).</p>
 */
public enum ZonesParticleRole {
    PRIMARY,
    SECONDARY,
    PREVIEW
}
