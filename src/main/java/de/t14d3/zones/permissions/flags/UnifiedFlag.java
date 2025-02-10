package de.t14d3.zones.permissions.flags;

import java.util.List;

public abstract class UnifiedFlag implements FlagInterface {
    private final List<String> validValues;
    private final boolean defaultValue;

    public UnifiedFlag(List<String> validValues, boolean defaultValue) {
        super();
        this.validValues = validValues;
        this.defaultValue = defaultValue;
    }

    @Override
    public List<String> getValidValues() {
        return this.validValues;
    }

    @Override
    public boolean getDefaultValue(Object... optional) {
        return defaultValue;
    }
}
