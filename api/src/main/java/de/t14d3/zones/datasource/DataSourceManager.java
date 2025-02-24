package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;

import java.util.List;

public class DataSourceManager {
    private AbstractDataSource currentDataSource;

    public DataSourceManager(Zones plugin) {
        DataSourceTypes type = DataSourceTypes.valueOf(
                plugin.getConfig().getString("storage.type", "YAML").toUpperCase());
        switch (type) {
            case YAML:
                this.currentDataSource = new YamlDataSource(plugin.getDataFolder(), plugin);
                break;
            case SQLITE, MYSQL, H2, POSTGRESQL, CUSTOM:
                this.currentDataSource = new SQLDataSource(plugin, type);
                break;
            default:
                throw new IllegalArgumentException("Invalid storage type: " + plugin.getConfig().getString("storage"));
        }
    }

    public void close() {
        currentDataSource.close();
    }

    public List<Region> loadRegions() {
        return currentDataSource.loadRegions();
    }

    public void saveRegions(List<Region> regions) {
        currentDataSource.saveRegions(regions);
    }

    public Region loadRegion(String key) {
        return currentDataSource.loadRegion(key);
    }

    public void saveRegion(String key, Region region) {
        currentDataSource.saveRegion(key, region);
    }

    public enum DataSourceTypes {
        YAML,
        MYSQL,
        SQLITE,
        H2,
        POSTGRESQL,
        CUSTOM
    }
}
