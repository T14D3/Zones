package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.Result;

import java.util.List;

public class DefaultPlayerFlagHandler extends PlayerFlag {

    public DefaultPlayerFlagHandler(boolean defaultValue, List<String> validValues) {
        super(validValues, defaultValue);
    }

    @Override
    public Result evaluate(Region region, String who, String permission, String type, Object... optionals) {
        if (super.getValidValues().contains(type.toLowerCase()) || super.getValidValues() == null ||
                type.equalsIgnoreCase("owner") || type.equalsIgnoreCase("admin") /*TODO: Fix this*/) {
            return super.evaluate(region, who, permission, type, optionals);
        } else {
            throw new IllegalArgumentException("Invalid type: " + type + " ValidValues: " + super.getValidValues());
        }
    }

    @Override
    public boolean getDefaultValue(Object... optional) {
        return super.getDefaultValue();
    }

    @Override
    public Result evaluate(Region region, String permission, String type, Object... optionals) {
        return super.evaluate(region, UNIVERSAL, permission, type, optionals);
    }

}
