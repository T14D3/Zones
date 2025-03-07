package de.t14d3.zones.objects;

import de.t14d3.zones.permissions.flags.Flags;

import java.util.ArrayList;
import java.util.List;

public class RegionFlagEntry {
    private final String flag;
    private final List<FlagValue> values;

    public RegionFlagEntry(String flag) {
        this.flag = flag;
        this.values = new ArrayList<>();
    }

    public RegionFlagEntry(String flag, String value, boolean inverted) {
        this.flag = flag;
        this.values = new ArrayList<>();
        this.setValue(value, inverted);
    }

    public RegionFlagEntry(String flag, List<FlagValue> values) {
        this.flag = flag;
        this.values = values;
    }

    public void setValue(String value, boolean inverted) {
        values.removeIf(val -> val.value.equalsIgnoreCase(value));
        values.add(new FlagValue(value, inverted));
    }

    public void removeValue(String value) {
        values.removeIf(val -> val.value.equalsIgnoreCase(value));
    }

    public Result getValue(String query) {
        for (FlagValue value : values) {
            if (value.inverted) {
                if (value.value.equalsIgnoreCase(query)) {
                    return Result.FALSE;
                }
            } else {
                if (value.value.equalsIgnoreCase(query)) {
                    return Result.TRUE;
                }
            }
        }
        return Result.UNDEFINED;
    }

    public String getFlagValue() {
        return flag;
    }

    public Flag getFlag() {
        return Flags.getFlag(flag);
    }

    public List<FlagValue> getValues() {
        return values;
    }


    public static class FlagValue {
        private final String value;
        private final boolean inverted;

        public FlagValue(String value, boolean inverted) {
            this.value = value;
            this.inverted = inverted;
        }

        public String getValue() {
            return value;
        }

        public boolean isInverted() {
            return inverted;
        }

        @Override
        public String toString() {
            return "FlagValue{" +
                    "value='" + value + '\'' +
                    ", inverted=" + inverted +
                    '}';
        }
    }
}
