package de.t14d3.zones.datasource;

import com.google.gson.Gson;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.shapes.CuboidShape;
import de.t14d3.zones.objects.shapes.GlobalShape;
import de.t14d3.zones.objects.shapes.ShapeProvider;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.RegionSecurityCodec;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                            "world VARCHAR(255), " +
                            "members TEXT, " +
                            "parent INT, " +
                            "priority INT, " +
                            "shape_type VARCHAR(255), " +
                            "shape_data TEXT" +
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
        String columns = "(\"key\", name, world, members, parent, priority, shape_type, shape_data)";
        String values = "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String updates;
        return switch (dbType) {
            case MYSQL -> {
                updates = "name=VALUES(name), world=VALUES(world), members=VALUES(members), parent=VALUES(parent), priority=VALUES(priority), "
                        + "shape_type=VALUES(shape_type), shape_data=VALUES(shape_data)";
                yield String.format("INSERT INTO %s %s %s ON DUPLICATE KEY UPDATE %s",
                        tableName, columns, values, updates);
            }
            case SQLITE -> String.format("INSERT OR REPLACE INTO %s %s %s", tableName, columns, values);
            case POSTGRESQL -> {
                updates = "name=EXCLUDED.name, world=EXCLUDED.world, members=EXCLUDED.members, parent=EXCLUDED.parent, priority=EXCLUDED.priority, "
                        + "shape_type=EXCLUDED.shape_type, shape_data=EXCLUDED.shape_data";
                yield String.format("INSERT INTO %s %s %s ON CONFLICT (key) DO UPDATE SET %s",
                        tableName, columns, values, updates);
            }
            case H2 -> String.format("MERGE INTO %s %s %s", tableName, columns, values);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        };
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (PreparedStatement statement = connection.prepareStatement(sql);    
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                Region region;
                try {
                    region = parseRegion(rs);
                } catch (SQLException e) {
                    zones.getLogger().error("Failed to parse a stored region row; skipping", e);
                    continue;
                }
                if (region != null) regions.add(region);
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
        String membersJson = rs.getString("members");

        RegionPermissions permissions;
        RegionMembership membership;
        if (membersJson != null && !membersJson.isBlank()) {
            RegionSecurityCodec.Persisted persisted = gson.fromJson(membersJson, RegionSecurityCodec.Persisted.class);
            RegionSecurityCodec.Decoded decoded = RegionSecurityCodec.decode(persisted);
            permissions = decoded.acl();
            membership = decoded.membership();
        } else {
            permissions = new RegionPermissions();
            membership = new RegionMembership();
        }
        int parentKey = rs.getInt("parent");
        RegionKey parent = parentKey != 0 ? RegionKey.fromInt(parentKey) : null;
        int priority = rs.getInt("priority");

        String shapeType = rs.getString("shape_type");
        if (shapeType == null || shapeType.isBlank()) {
            zones.getLogger().error("Region {} is missing required shape_type; skipping", key);
            return null;
        }
        String shapeData = rs.getString("shape_data");
        if (shapeData == null || shapeData.isBlank()) {
            zones.getLogger().error("Region {} is missing required shape_data; skipping", key);
            return null;
        }

        ShapeProvider shape;
        try {
            switch (shapeType) {
                case "global" -> shape = GlobalShape.fromJson(shapeData);
                case "cuboid" -> shape = CuboidShape.fromJson(shapeData);
                default -> {
                    zones.getLogger().error("Unknown shape type {} for region {}; skipping", shapeType, key);
                    return null;
                }
            }
        } catch (Exception e) {
            zones.getLogger().error("Failed to parse shape for region {}; skipping", key, e);
            return null;
        }

        return new Region(name, shape, permissions, membership, RegionKey.fromInt(key), parent, priority);
    }


    @Override
    public void saveRegions(List<Region> regions) {
        String sql = buildUpsertSQL();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {       
            for (Region region : regions) {
                stmt.setInt(1, region.getKey().getValue());
                stmt.setString(2, region.getName());
                stmt.setString(3, region.getWorld() != null ? region.getWorld().identifier() : null);
                stmt.setString(4,
                        gson.toJson(RegionSecurityCodec.encode(region.getPermissions(), region.getMembership())));
                stmt.setInt(5, region.getParent() != null ? region.getParent().getValue() : 0);
                stmt.setInt(6, region.getPriority());

                // Shape data
                ShapeProvider shape = region.getShape();
                stmt.setString(7, shape.getShapeType());
                stmt.setString(8, shape.toJson());

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
            stmt.setString(3, region.getWorld() != null ? region.getWorld().identifier() : null);
            stmt.setString(4, gson.toJson(RegionSecurityCodec.encode(region.getPermissions(), region.getMembership())));
            stmt.setInt(5, region.getParent() != null ? region.getParent().getValue() : 0);
            stmt.setInt(6, region.getPriority());

            // Shape data
            ShapeProvider shape = region.getShape();
            stmt.setString(7, shape.getShapeType());
            stmt.setString(8, shape.toJson());

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
