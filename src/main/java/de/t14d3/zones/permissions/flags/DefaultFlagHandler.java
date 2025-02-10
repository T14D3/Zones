package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.Result;

import java.util.List;

public class DefaultFlagHandler extends UnifiedFlag implements FlagInterface {

    public DefaultFlagHandler(boolean defaultValue, List<String> validValues) {
        super(validValues, defaultValue);
    }


    @Override
    public Result evaluate(Region region, String who, String permission, String type, Object... optionals) {
        if (getValidValues().contains(type.toLowerCase()) || getValidValues() == null || getValidValues().isEmpty()) {
            return super.evaluate(region, who, permission, type, optionals);
        } else {
            throw new IllegalArgumentException("Invalid type: " + type + " ValidValues: " + getValidValues());
        }
    }
}
