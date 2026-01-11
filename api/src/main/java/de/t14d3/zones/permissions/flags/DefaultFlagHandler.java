package de.t14d3.zones.permissions.flags;

import java.util.List;

public class DefaultFlagHandler implements IFlagHandler {
    private final boolean defaultValue;
    private final List<String> validValues;

    public DefaultFlagHandler(boolean defaultValue, List<String> validValues) {
        super();
        this.defaultValue = defaultValue;
        this.validValues = validValues;
    }

    @Override
    public boolean getDefaultValue(FlagContext context) {
        return this.defaultValue;
    }

    @Override
    public List<String> getValidValues() {
        return this.validValues;
    }

}
