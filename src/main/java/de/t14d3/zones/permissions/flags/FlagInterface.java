package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.Region;
import de.t14d3.zones.permissions.Result;

import java.util.List;

public interface FlagInterface {
    String UNIVERSAL = "universal";

    List<String> getValidValues();

    boolean getDefaultValue(Object... optional);

    default Result evaluate(Region region, String permission, String type, Object... optionals) {
        return evaluate(region, UNIVERSAL, permission, type, optionals);
    }

    Result evaluate(Region region, String who, String permission, String type, Object... optionals);

}
