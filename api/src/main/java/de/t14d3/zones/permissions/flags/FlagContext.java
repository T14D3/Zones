package de.t14d3.zones.permissions.flags;

import org.jetbrains.annotations.Nullable;

/**
 * Additional context for permission checks.
 *
 * <p>Zones' permission engine primarily matches a single {@code type} key against patterns stored in regions. Some
 * flags still need extra information to decide their <i>fallback/default</i> behavior when no region rule applies.
 * This replaces the old {@code Object... extra} "magic values" API with a small, typed carrier.</p>
 */
public record FlagContext(
        @Nullable Boolean defaultDecisionOverride,
        @Nullable String reasonKey,
        @Nullable String causeKey,
        @Nullable String damageTypeKey
) {
    private static final FlagContext NONE = new FlagContext(null, null, null, null);

    public static FlagContext none() {
        return NONE;
    }

    public static FlagContext defaultDecision(boolean decision) {
        return new FlagContext(decision, null, null, null);
    }

    public FlagContext withReason(@Nullable String reason) {
        return new FlagContext(defaultDecisionOverride, reason, causeKey, damageTypeKey);
    }

    public FlagContext withCause(@Nullable String cause) {
        return new FlagContext(defaultDecisionOverride, reasonKey, cause, damageTypeKey);
    }

    public FlagContext withDamageType(@Nullable String damageType) {
        return new FlagContext(defaultDecisionOverride, reasonKey, causeKey, damageType);
    }
}

