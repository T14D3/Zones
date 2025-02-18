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

public class SqliteDataSource extends AbstractDataSource {
    private Connection connection;
    private Zones plugin;
    private Gson gson = new Gson();

    /**
     * Creates a new SQLite data source.
     *
     * @param plugin The plugin instance.
     */
    public SqliteDataSource(Zones plugin) {
        super(plugin);
        this.plugin = plugin;
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:plugins/Zones/regions.db");

            String createTableSQL =
                    "CREATE TABLE IF NOT EXISTS regions (" +
                            "key INTEGER PRIMARY KEY, " +
                            "name TEXT, " +
                            "minX INTEGER, " +
                            "minY INTEGER, " +
                            "minZ INTEGER, " +
                            "maxX INTEGER, " +
                            "maxY INTEGER, " +
                            "maxZ INTEGER, " +
                            "world TEXT, " +
                            "members TEXT, " +
                            "parent TEXT, " +
                            "priority INTEGER" +
                            ")";
            connection.prepareStatement(createTableSQL).execute();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize SQLite database! Error: " + e.getMessage());
            if (plugin.debug) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM regions");
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
        String sql = "INSERT OR REPLACE INTO regions VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
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
                stmt.setInt(11, region.getParent() != null ? region.getParent().getValue() : null);
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
        String sql = "SELECT * FROM regions WHERE key = ?";
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
        String sql = "INSERT OR REPLACE INTO regions VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
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
            stmt.setInt(11, region.getParent() != null ? region.getParent().getValue() : null);
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