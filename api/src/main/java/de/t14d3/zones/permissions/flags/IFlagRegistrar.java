package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.ZonesPlatform;

/**
 * Interface for registering platform-specific logic for flags, such as event handlers.
 */
public interface IFlagRegistrar {

    /**
     * Registers the flag's platform-specific logic.
     *
     * @param platform The platform to register with.
     */
    void register(ZonesPlatform platform);
}
