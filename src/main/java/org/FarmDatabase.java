/*
 * Ruth Forzani
 * CEN 3024C - Software Development I
 * DMS Project - Phase 4 (SQLite Back End)
 * Farm Finder
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FarmDatabase implements AutoCloseable {
    private Connection connection;
    private final FarmValidator validator = new FarmValidator();

    /** connect to an existing database. */
    public void connect(String databasePath) {
        if (databasePath == null || databasePath.isBlank()) {
            throw new IllegalArgumentException("Choose an SQLite database file.");
        }
        Path path = Path.of(databasePath).toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Database file not found: " + path);
        }

        Connection candidate = null;
        try {
            // dependencies
            Class.forName("org.sqlite.JDBC");
            candidate = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (Statement statement = candidate.createStatement()) {
                statement.execute("PRAGMA busy_timeout = 5000");
                try (ResultSet ignored = statement.executeQuery(
                        "SELECT farm_id, name, city, products, season, organic, "
                                + "distance_miles, last_updated FROM farms LIMIT 0")) {
                }
            }
            close();
            connection = candidate;
        } catch (ClassNotFoundException exception) {
            closeQuietly(candidate);
            throw new IllegalStateException(
                    "SQLite JDBC driver missing. Add org.xerial:sqlite-jdbc.", exception);
        } catch (SQLException exception) {
            closeQuietly(candidate);
            throw new IllegalStateException("Cannot connect to Farm Finder database: "
                    + exception.getMessage(), exception);
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

    private Connection database() {
        if (!isConnected()) {
            throw new IllegalStateException("Connect to an SQLite database first.");
        }
        return connection;
    }

    private Farm fromRow(ResultSet row) throws SQLException {
        return new Farm(row.getInt("farm_id"), row.getString("name"),
                row.getString("city"), row.getString("products"),
                row.getString("season"), row.getInt("organic") == 1,
                row.getDouble("distance_miles"),
                LocalDate.parse(row.getString("last_updated")));
    }

    /** read the current data from the database. */
    public ArrayList<Farm> getAllFarms() {
        ArrayList<Farm> farms = new ArrayList<>();
        String sql = "SELECT * FROM farms ORDER BY farm_id";
        try (PreparedStatement statement = database().prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                farms.add(fromRow(rows));
            }
            return farms;
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    public Farm findFarmById(int id) {
        String sql = "SELECT * FROM farms WHERE farm_id = ?";
        try (PreparedStatement statement = database().prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? fromRow(rows) : null;
            }
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    /** INSERT is immediately done to the database. */
    public boolean addFarm(Farm farm) {
        checkFarm(farm);
        String sql = "INSERT INTO farms (farm_id, name, city, products, season, "
                + "organic, distance_miles, last_updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = database().prepareStatement(sql)) {
            putFarm(statement, farm);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    /** Update all editable fields all at once */
    public boolean updateFarm(int oldId, Farm edited) {
        checkFarm(edited);
        String sql = "UPDATE farms SET farm_id = ?, name = ?, city = ?, products = ?, "
                + "season = ?, organic = ?, distance_miles = ?, last_updated = ? "
                + "WHERE farm_id = ?";
        try (PreparedStatement statement = database().prepareStatement(sql)) {
            putFarm(statement, edited);
            statement.setInt(9, oldId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    /** DELETE is immediately done to the SQLite database. */
    public boolean removeFarm(int id) {
        String sql = "DELETE FROM farms WHERE farm_id = ?";
        try (PreparedStatement statement = database().prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    /** filtering in the database */
    public ArrayList<Farm> findLocalFarms(String product, double maxMiles, boolean organicOnly) {
        if (!validator.isNotBlank(product) || !validator.isValidDistance(maxMiles)) {
            throw new IllegalArgumentException("Enter a product and miles from 0 to 200.");
        }
        ArrayList<Farm> matches = new ArrayList<>();
        String sql = "SELECT * FROM farms WHERE distance_miles <= ? "
                + "AND (? = 0 OR organic = 1) "
                + "AND instr(';' || replace(lower(products), ' ', '') || ';', "
                + "';' || replace(lower(?), ' ', '') || ';') > 0 "
                + "ORDER BY distance_miles ASC, farm_id ASC";
        try (PreparedStatement statement = database().prepareStatement(sql)) {
            statement.setDouble(1, maxMiles);
            statement.setInt(2, organicOnly ? 1 : 0);
            statement.setString(3, product.trim());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    matches.add(fromRow(rows));
                }
            }
            return matches;
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    public int getFarmCount() {
        try (PreparedStatement statement = database().prepareStatement(
                "SELECT COUNT(*) FROM farms");
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        } catch (SQLException exception) {
            throw sqlError(exception);
        }
    }

    public double calculateAverageDistance(List<Farm> matches) {
        if (matches == null || matches.isEmpty()) return 0.0;
        double sum = 0;
        for (Farm farm : matches) sum += farm.getDistanceMiles();
        return sum / matches.size();
    }

    private void checkFarm(Farm farm) {
        String message = validator.getErrorMessage(farm);
        if (!message.isEmpty()) throw new IllegalArgumentException(message);
    }

    private void putFarm(PreparedStatement statement, Farm farm) throws SQLException {
        statement.setInt(1, farm.getFarmId());
        statement.setString(2, farm.getName());
        statement.setString(3, farm.getCity());
        statement.setString(4, farm.getProducts());
        statement.setString(5, farm.getSeason());
        statement.setInt(6, farm.isOrganic() ? 1 : 0);
        statement.setDouble(7, farm.getDistanceMiles());
        statement.setString(8, farm.getLastUpdated().toString());
    }

    private IllegalStateException sqlError(SQLException exception) {
        return new IllegalStateException("SQLite error: " + exception.getMessage(), exception);
    }

    @Override
    public void close() {
        if (connection != null) {
            closeQuietly(connection);
            connection = null;
        }
    }

    private void closeQuietly(Connection c) {
        if (c != null) {
            try { c.close(); } catch (SQLException ignored) { }
        }
    }
}
