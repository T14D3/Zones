package de.t14d3.zones.objects;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.permissions.flags.DefaultFlagHandler;
import de.t14d3.zones.permissions.flags.FlagContext;
import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.flags.IFlagHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Flag definition used by Zones' permission engine.
 *
 * <p>Flags are metadata: value kind, defaults, and suggestions. Permission evaluation and caching live in the
 * permission engine; flags do not "execute" permissions.</p>
 */
public final class Flag {
    @FunctionalInterface
    public interface CanSetRule {
        boolean canSet(@Nullable RPlayer player, @NotNull ZonesPlatform platform);
    }

    private static final IFlagHandler DEFAULT_HANDLER = new DefaultFlagHandler(true, List.of());
    private static final CanSetRule ALWAYS_CAN_SET = (player, platform) -> true;

    private final int id;
    private final String name;
    private final String description;
    private final IFlagHandler handler;
    private final CanSetRule canSetRule;
    private final FlagValueKind valueKind;

    public Flag(String name, String description) {
        this(-1, name, description, null, null, FlagValueKind.TARGET_DECISION);
    }

    public Flag(String name, String description, IFlagHandler handler, CanSetRule canSetRule) {
        this(-1, name, description, handler, canSetRule, FlagValueKind.TARGET_DECISION);
    }

    public Flag(int id, @NotNull String name, @NotNull String description, @Nullable IFlagHandler handler, @Nullable CanSetRule canSetRule, @Nullable FlagValueKind valueKind) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name").trim();
        this.description = Objects.requireNonNull(description, "description");
        this.canSetRule = Objects.requireNonNullElse(canSetRule, ALWAYS_CAN_SET);
        this.handler = Objects.requireNonNullElse(handler, DEFAULT_HANDLER);
        this.valueKind = Objects.requireNonNullElse(valueKind, FlagValueKind.TARGET_DECISION);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public FlagValueKind valueKind() {
        return valueKind;
    }

    public IFlagHandler handler() {
        return handler;
    }

    public CanSetRule canSetRule() {
        return canSetRule;
    }

    public boolean getDefaultValue() {
        return getDefaultValue(FlagContext.none());
    }

    public boolean getDefaultValue(FlagContext context) {
        return handler.getDefaultValue(context);
    }

    public List<String> getValidValues() {
        return handler.getValidValues();
    }

    public boolean canSet(RPlayer player, ZonesPlatform platform) {
        return canSetRule.canSet(player, Objects.requireNonNull(platform, "platform"));
    }
}
