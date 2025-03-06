package de.t14d3.zones.datasource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.RegionFlagEntry;
import de.t14d3.zones.objects.World;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLDataSource extends AbstractDataSource {
    private Connection connection;
    private final Zones zones;
    private final Gson gson = new Gson();
    private final String tableName;
    private final DataSourceManager.DataSourceTypes dbType;

    public SQLDataSource(Zones zones, DataSourceManager.DataSourceTypes type) {
        super(zones);
        this.zones = zones;
        this.dbType = type;
        this.tableName = zones.getConfig().getString("storage.table", "regions");
        switch (type) {
            case MYSQL -> {
                try {
                    String host = zones.getConfig().getString("storage.mysql.host", "localhost:3306");
                    String database = zones.getConfig().getString("storage.mysql.database", "zones");
                    String user = zones.getConfig().getString("storage.mysql.user", "root");
                    String password = zones.getConfig().getString("storage.mysql.password", "CHANGEME");
                    String options = zones.getConfig()
                            .getString("storage.mysql.options", "?serverTimezone=UTC&autoReconnect=true");
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    String url = "jdbc:mysql://" + host + "/" + database + options;
                    this.connection = DriverManager.getConnection(url, user, password);
                } catch (Exception e) {
                    zones.getLogger().error("Failed to initialize MySQL database! Error: {}", e.getMessage());
                    if (zones.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case SQLITE -> {
                try {
                    Class.forName("org.sqlite.JDBC");
                    this.connection = DriverManager.getConnection("jdbc:sqlite:./plugins/Zones/regions.sqlite.db");
                } catch (Exception e) {
                    zones.getLogger().error("Failed to initialize SQLite database! Error: {}", e.getMessage());
                    if (zones.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case H2 -> {
                try {
                    Class.forName("org.h2.Driver");
                    this.connection = DriverManager.getConnection("jdbc:h2:file:./plugins/Zones/regions.h2");
                } catch (Exception e) {
                    zones.getLogger().error("Failed to initialize H2 database! Error: {}", e.getMessage());
                    if (zones.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case POSTGRESQL -> {
                try {
                    String host = zones.getConfig().getString("storage.postgresql.host", "localhost:5432");
                    String database = zones.getConfig().getString("storage.postgresql.database", "zones");
                    String user = zones.getConfig().getString("storage.postgresql.user", "root");
                    String password = zones.getConfig().getString("storage.postgresql.password", "CHANGEME");
                    String options = zones.getConfig()
                            .getString("storage.postgresql.options", "?serverTimezone=UTC&autoReconnect=true");
                    Class.forName("org.postgresql.Driver");
                    String url = "jdbc:postgresql://" + host + "/" + database + options;
                    this.connection = DriverManager.getConnection(url, user, password);
                } catch (Exception e) {
                    zones.getLogger().error("Failed to initialize PostgreSQL database! Error: {}", e.getMessage());
                    if (zones.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case CUSTOM -> {
                try {
                    String url = zones.getConfig().getString("storage.custom.url");
                    String driver = zones.getConfig().getString("storage.custom.driver");
                    Class.forName(driver);
                    this.connection = DriverManager.getConnection(url);
                } catch (Exception e) {
                    zones.getLogger().error("Failed to initialize custom database! Error: {}", e.getMessage());
                    if (zones.debug) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            String createTableSQL =
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                            "\"key\" INT PRIMARY KEY, " +
                            "name VARCHAR(255), " +
                            "minX INT, " +
                            "minY INT, " +
                            "minZ INT, " +
                            "maxX INT, " +
                            "maxY INT, " +
                            "maxZ INT, " +
                            "world VARCHAR(255), " +
                            "members TEXT, " +
                            "parent INT, " +
                            "priority INT" +
                            ")";
            connection.prepareStatement(createTableSQL).execute();
        } catch (SQLException e) {
            zones.getLogger().error("Failed to create table! Error: {}", e.getMessage());
            if (zones.debug) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() {
        try {
            this.connection.close();
        } catch (SQLException e) {
            zones.getLogger().error("Error closing the database connection: {}", e.getMessage());
            if (zones.debug) {
                e.printStackTrace();
            }
        }
    }

    private String buildUpsertSQL() {
        String columns = "(\"key\", name, minX, minY, minZ, maxX, maxY, maxZ, world, members, parent, priority)";
        String values = "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String updates;
        switch (dbType) {
            case MYSQL:
                updates = "name=VALUES(name), minX=VALUES(minX), minY=VALUES(minY), minZ=VALUES(minZ), " +
                        "maxX=VALUES(maxX), maxY=VALUES(maxY), maxZ=VALUES(maxZ), world=VALUES(world), " +
                        "members=VALUES(members), parent=VALUES(parent), priority=VALUES(priority)";
                return String.format("INSERT INTO %s %s %s ON DUPLICATE KEY UPDATE %s",
                        tableName, columns, values, updates);
            case SQLITE:
                return String.format("INSERT OR REPLACE INTO %s %s %s", tableName, columns, values);
            case POSTGRESQL:
                updates = "name=EXCLUDED.name, minX=EXCLUDED.minX, minY=EXCLUDED.minY, " +
                        "minZ=EXCLUDED.minZ, maxX=EXCLUDED.maxX, maxY=EXCLUDED.maxY, maxZ=EXCLUDED.maxZ, " +
                        "world=EXCLUDED.world, members=EXCLUDED.members, parent=EXCLUDED.parent, priority=EXCLUDED.priority";
                return String.format("INSERT INTO %s %s %s ON CONFLICT (key) DO UPDATE SET %s",
                        tableName, columns, values, updates);
            case H2:
                return String.format("MERGE INTO %s %s %s", tableName, columns, values);
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Region region = parseRegion(rs);
                zones.getRegionManager().addRegion(region);
                regions.add(region);
            }
        } catch (SQLException e) {
            zones.getLogger().error("Failed to load regions! Error: {}", e.getMessage());
            if (zones.debug) {
                e.printStackTrace();
            }
        }
        return regions;
    }

    private Region parseRegion(ResultSet rs) throws SQLException {
        int key = rs.getInt("key");
        String name = rs.getString("name");
        BlockLocation min = new BlockLocation(
                rs.getInt("minX"),
                rs.getInt("minY"),
                rs.getInt("minZ")
        );
        BlockLocation max = new BlockLocation(
                rs.getInt("maxX"),
                rs.getInt("maxY"),
                rs.getInt("maxZ")
        );
        World world = Zones.getInstance().getPlatform().getWorld(rs.getString("world"));
        String membersJson = rs.getString("members");
        Map<String, List<RegionFlagEntry>> members = gson.fromJson(membersJson,
                new TypeToken<Map<String, List<RegionFlagEntry>>>() {
                }.getType());
        int parentKey = rs.getInt("parent");
        RegionKey parent = parentKey != 0 ? RegionKey.fromInt(parentKey) : null;
        int priority = rs.getInt("priority");

        return new Region(name, min, max, world, members,
                RegionKey.fromInt(key), parent, priority);
    }

    @Override
    public void saveRegions(List<Region> regions) {
        String sql = buildUpsertSQL();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Region region : regions) {
                stmt.setInt(1, region.getKey().getValue());
                stmt.setString(2, region.getName());
                BlockLocation min = region.getMin();
                stmt.setInt(3, min.getX());
                stmt.setInt(4, min.getY());
                stmt.setInt(5, min.getZ());
                BlockLocation max = region.getMax();
                stmt.setInt(6, max.getX());
                stmt.setInt(7, max.getY());
                stmt.setInt(8, max.getZ());
                stmt.setString(9, region.getWorld() != null ? region.getWorld().getName() : null);
                stmt.setString(10, gson.toJson(region.getMembers()));
                stmt.setInt(11, region.getParent() != null ? region.getParent().getValue() : 0);
                stmt.setInt(12, region.getPriority());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            zones.getLogger().error("Failed to save regions! Error: {}", e.getMessage());
            if (zones.debug) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Region loadRegion(String key) {
        String sql = "SELECT * FROM " + tableName + " WHERE key = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return parseRegion(rs);
            }
        } catch (SQLException e) {
            zones.getLogger().error("Failed to load region {}! Error: {}", key, e.getMessage());
            if (zones.debug) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void saveRegion(String key, Region region) {
        String sql = buildUpsertSQL();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, RegionKey.fromString(key).getValue());
            stmt.setString(2, region.getName());
            BlockLocation min = region.getMin();
            stmt.setInt(3, min.getX());
            stmt.setInt(4, min.getY());
            stmt.setInt(5, min.getZ());
            BlockLocation max = region.getMax();
            stmt.setInt(6, max.getX());
            stmt.setInt(7, max.getY());
            stmt.setInt(8, max.getZ());
            stmt.setString(9, region.getWorld() != null ? region.getWorld().getName() : null);
            stmt.setString(10, gson.toJson(region.getMembers()));
            stmt.setInt(11, region.getParent() != null ? region.getParent().getValue() : 0);
            stmt.setInt(12, region.getPriority());
            stmt.executeUpdate();
        } catch (SQLException e) {
            zones.getLogger()
                    .error("Failed to save region {}! Error: {}", region.getKey().toString(), e.getMessage());
            if (zones.debug) {
                e.printStackTrace();
            }
        }
    }
}
