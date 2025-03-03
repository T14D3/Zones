package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;

import java.util.List;

public abstract class AbstractDataSource {

    public AbstractDataSource(Zones plugin) {
    }

    public abstract List<Region> loadRegions();

    public abstract void saveRegions(List<Region> regions);

    public abstract Region loadRegion(String key);

    public abstract void saveRegion(String key, Region region);

    public void close() {

    }
}
