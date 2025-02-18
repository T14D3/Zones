package de.t14d3.zones.permissions.flags;

import java.util.List;

public class DefaultFlagHandler implements IFlagHandler {
    private boolean defaultValue;
    private List<String> validValues;

    public DefaultFlagHandler(boolean defaultValue, List<String> validValues) {
        super();
        this.defaultValue = defaultValue;
        this.validValues = validValues;
    }

    @Override
    public boolean getDefaultValue(Object... optional) {
        if (optional.length == 0) {
            return this.defaultValue;
        } else return optional[0].toString().equalsIgnoreCase("+universal");
    }

    @Override
    public List<String> getValidValues() {
        return this.validValues;
    }

}
