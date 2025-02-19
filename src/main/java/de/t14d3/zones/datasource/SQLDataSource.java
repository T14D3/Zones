package de.t14d3.zones.datasource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.util.BlockVector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQLDataSource extends AbstractDataSource {
    private Connection connection;
    private final Zones plugin;
    private final Gson gson = new Gson();
    private final String tableName;

    public SQLDataSource(Zones plugin, DataSourceManager.DataSourceTypes type) {
        super(plugin);
        this.plugin = plugin;
        this.tableName = plugin.getConfig().getString("storage.table", "regions");
        switch (type) {
            case MYSQL -> {
                try {
                    String host = plugin.getConfig().getString("storage.mysql.host", "localhost:3306");
                    String database = plugin.getConfig().getString("storage.mysql.database", "zones");
                    String user = plugin.getConfig().getString("storage.mysql.user", "root");
                    String password = plugin.getConfig().getString("storage.mysql.password", "CHANGEME");
                    String options = plugin.getConfig()
                            .getString("storage.mysql.options", "?serverTimezone=UTC&autoReconnect=true");
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    String url = "jdbc:mysql://" + host + "/" + database + options;
                    this.connection = DriverManager.getConnection(url, user, password);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to initialize MySQL database! Error: " + e.getMessage());
                    if (plugin.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case SQLITE -> {
                try {
                    Class.forName("org.sqlite.JDBC");
                    this.connection = DriverManager.getConnection("jdbc:sqlite:plugins/Zones/regions.sqlite.db");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to initialize SQLite database! Error: " + e.getMessage());
                    if (plugin.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case H2 -> {
                try {
                    Class.forName("org.h2.Driver");
                    this.connection = DriverManager.getConnection("jdbc:h2:file:plugins/Zones/regions.h2.db");
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to initialize H2 database! Error: " + e.getMessage());
                    if (plugin.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case POSTGRESQL -> {
                try {
                    String host = plugin.getConfig().getString("storage.postgresql.host", "localhost:5432");
                    String database = plugin.getConfig().getString("storage.postgresql.database", "zones");
                    String user = plugin.getConfig().getString("storage.postgresql.user", "root");
                    String password = plugin.getConfig().getString("storage.postgresql.password", "CHANGEME");
                    String options = plugin.getConfig()
                            .getString("storage.postgresql.options", "?serverTimezone=UTC&autoReconnect=true");
                    Class.forName("org.postgresql.Driver");
                    String url = "jdbc:postgresql://" + host + "/" + database + options;
                    this.connection = DriverManager.getConnection(url, user, password);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to initialize PostgreSQL database! Error: " + e.getMessage());
                    if (plugin.debug) {
                        e.printStackTrace();
                    }
                }
            }
            case CUSTOM -> {
                try {
                    String url = plugin.getConfig().getString("storage.custom.url");
                    String driver = plugin.getConfig().getString("storage.custom.driver");
                    Class.forName(driver);
                    this.connection = DriverManager.getConnection(url);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to initialize custom database! Error: " + e.getMessage());
                    if (plugin.debug) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            String createTableSQL =
                    "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                            "key INT PRIMARY KEY, " +
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
            plugin.getLogger().severe("Failed to create table! Error: " + e.getMessage());
            if (plugin.debug) {
                e.printStackTrace();
            }
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
                plugin.getRegionManager().addRegion(region);
                regions.add(region);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load regions! Error: " + e.getMessage());
            if (plugin.debug) {
                e.printStackTrace();
            }
        }
        return regions;
    }

    private Region parseRegion(ResultSet rs) throws SQLException {
        int key = rs.getInt("key");
        String name = rs.getString("name");
        BlockVector min = new BlockVector(
                rs.getInt("minX"),
                rs.getInt("minY"),
                rs.getInt("minZ")
        );
        BlockVector max = new BlockVector(
                rs.getInt("maxX"),
                rs.getInt("maxY"),
                rs.getInt("maxZ")
        );
        World world = Bukkit.getWorld(rs.getString("world"));
        String membersJson = rs.getString("members");
        Map<String, Map<String, String>> members = gson.fromJson(membersJson,
                new TypeToken<Map<String, Map<String, String>>>() {
                }.getType());
        int parentKey = rs.getInt("parent");
        RegionKey parent = parentKey != 0 ? RegionKey.fromInt(parentKey) : null;
        int priority = rs.getInt("priority");

        return new Region(name, min, max, world, members,
                RegionKey.fromInt(key), parent, priority);
    }

    @Override
    public void saveRegions(List<Region> regions) {
        String sql = "INSERT OR REPLACE INTO " + tableName + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Region region : regions) {
                stmt.setInt(1, region.getKey().getValue());
                stmt.setString(2, region.getName());
                BlockVector min = region.getMin();
                stmt.setInt(3, min.getBlockX());
                stmt.setInt(4, min.getBlockY());
                stmt.setInt(5, min.getBlockZ());
                BlockVector max = region.getMax();
                stmt.setInt(6, max.getBlockX());
                stmt.setInt(7, max.getBlockY());
                stmt.setInt(8, max.getBlockZ());
                stmt.setString(9, region.getWorld() != null ? region.getWorld().getName() : null);
                stmt.setString(10, gson.toJson(region.getMembers()));
                stmt.setInt(11, region.getParent() != null ? region.getParent().getValue() : 0);
                stmt.setInt(12, region.getPriority());
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save regions! Error: " + e.getMessage());
            if (plugin.debug) {
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
            plugin.getLogger().severe("Failed to load region " + key + "! Error: " + e.getMessage());
            if (plugin.debug) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void saveRegion(String key, Region region) {
        String sql = "INSERT OR REPLACE INTO " + tableName + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, RegionKey.fromString(key).getValue());
            stmt.setString(2, region.getName());
            BlockVector min = region.getMin();
            stmt.setInt(3, min.getBlockX());
            stmt.setInt(4, min.getBlockY());
            stmt.setInt(5, min.getBlockZ());
            BlockVector max = region.getMax();
            stmt.setInt(6, max.getBlockX());
            stmt.setInt(7, max.getBlockY());
            stmt.setInt(8, max.getBlockZ());
            stmt.setString(9, region.getWorld() != null ? region.getWorld().getName() : null);
            stmt.setString(10, gson.toJson(region.getMembers()));
            stmt.setInt(11, region.getParent() != null ? region.getParent().getValue() : 0);
            stmt.setInt(12, region.getPriority());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger()
                    .severe("Failed to save region " + region.getKey().toString() + "! Error: " + e.getMessage());
            if (plugin.debug) {
                e.printStackTrace();
            }
        }
    }
}
