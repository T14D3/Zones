package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.Result;

import java.util.List;

public class DefaultUniversalFlagHandler extends UniversalFlag {

    public DefaultUniversalFlagHandler(boolean defaultValue, List<String> validValues) {
        super(validValues, defaultValue);
    }

    @Override
    public Result evaluate(Region region, String permission, String type, Object... optionals) {
        if (super.getValidValues().contains(type.toLowerCase()) || super.getValidValues() == null) {
            return super.evaluate(region, permission, type, optionals);
        } else {
            throw new IllegalArgumentException("Invalid type: " + type + " ValidValues: " + super.getValidValues());
        }
    }

    @Override
    public Result evaluate(Region region, String who, String permission, String type, Object... optionals) {
        throw new UnsupportedOperationException("Cannot evaluate universal flag for a specific player");
    }

    @Override
    public boolean getDefaultValue(Object... optional) {
        return super.getDefaultValue();
    }
}
