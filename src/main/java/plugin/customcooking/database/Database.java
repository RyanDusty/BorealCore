package plugin.customcooking.database;

import org.bukkit.entity.Player;
import plugin.customcooking.CustomCooking;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import static plugin.customcooking.functions.jade.JadeManager.jadeSources;

public abstract class Database {
    public static CustomCooking plugin;
    public Connection connection;
    public String table = "jade_transactions";
    public int tokens = 0;

    public Database(CustomCooking instance) {
        plugin = CustomCooking.getInstance();
    }

    public Connection getSQLConnection() {
        return null;
    }

    public abstract void dbload();

    public void initialize() {
        this.connection = this.getSQLConnection();

        try {
            PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM " + this.table + " WHERE player = ?");
            ResultSet rs = ps.executeQuery();
            this.close(ps, rs);
        } catch (SQLException var3) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retreive connection", var3);
        }

    }

    private void closeResources(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
        }
    }

    public Integer getPlayerData(String playerName, String column) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = this.getSQLConnection();
            ps = conn.prepareStatement("SELECT jade FROM jade_totals WHERE player = ?;");
            ps.setString(1, playerName.toLowerCase());
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("jade");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }

        return 0;
    }

    public void addTransaction(Player player, double amount, String source, LocalDateTime timestamp) {
        Connection conn = null;
        PreparedStatement psTransaction = null;
        PreparedStatement psTotals = null;

        try {
            conn = this.getSQLConnection();

            // Add the transaction to jade_transactions
            String transactionQuery = "INSERT INTO jade_transactions (player, amount, source, timestamp) VALUES (?, ?, ?, ?);";
            psTransaction = conn.prepareStatement(transactionQuery);
            psTransaction.setString(1, player.getName().toLowerCase());
            psTransaction.setDouble(2, amount);
            psTransaction.setString(3, source);
            psTransaction.setTimestamp(4, Timestamp.valueOf(timestamp));
            psTransaction.executeUpdate();

            // Update or insert the player's total in jade_totals
            String totalsQuery = """
                        INSERT INTO jade_totals (player, jade)
                        VALUES (?, ?)
                        ON CONFLICT(player) DO UPDATE SET jade = jade + ?;
                    """;
            psTotals = conn.prepareStatement(totalsQuery);
            psTotals.setString(1, player.getName().toLowerCase());
            psTotals.setDouble(2, amount);
            psTotals.setDouble(3, amount);
            psTotals.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (psTransaction != null) psTransaction.close();
                if (psTotals != null) psTotals.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }
    }


    public int getJadeForPlayer(Player player) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = this.getSQLConnection();
            String query = "SELECT jade FROM jade_totals WHERE player = ?;";
            ps = conn.prepareStatement(query);
            ps.setString(1, player.getName().toLowerCase());
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("jade");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }

        return 0; // Return 0 if no record is found.
    }

    public int getTotalJadeFromSource(String source) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        int var6;
        try {
            conn = this.getSQLConnection();
            String query = "SELECT SUM(amount) AS total FROM jade_transactions WHERE source = ?;";
            ps = conn.prepareStatement(query);
            ps.setString(1, source);
            rs = ps.executeQuery();
            if (!rs.next()) {
                return 0;
            }

            var6 = rs.getInt("total");
        } catch (SQLException var17) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), var17);
            return 0;
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (ps != null) {
                    ps.close();
                }

                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException var16) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), var16);
            }

        }

        return var6;
    }

    public List<LocalDateTime> getRecentPositiveTransactionTimestamps(Player player, String source) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<LocalDateTime> timestamps = new ArrayList<>();

        try {
            conn = this.getSQLConnection();
            String query = "SELECT timestamp FROM jade_transactions WHERE player = ? AND source = ? AND amount > 0 AND timestamp >= ? ORDER BY timestamp DESC;";
            ps = conn.prepareStatement(query);
            ps.setString(1, player.getName().toLowerCase());
            ps.setString(2, source);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().minus(24, ChronoUnit.HOURS)));
            rs = ps.executeQuery();
            while (rs.next()) {
                timestamps.add(rs.getTimestamp("timestamp").toLocalDateTime());
            }
        } catch (SQLException var17) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), var17);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (ps != null) {
                    ps.close();
                }

                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException var16) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), var16);
            }
        }

        return timestamps;
    }

    public boolean isOnCooldown(Player player, String source) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = this.getSQLConnection();
            String query = "SELECT timestamp FROM jade_transactions WHERE player = ? AND source = ? ORDER BY timestamp DESC LIMIT 1;";
            ps = conn.prepareStatement(query);
            ps.setString(1, player.getName().toLowerCase());
            ps.setString(2, source);
            rs = ps.executeQuery();

            if (rs.next()) {
                LocalDateTime lastTransactionTime = rs.getTimestamp("timestamp").toLocalDateTime();
                LocalDateTime cooldownEndTime = lastTransactionTime.plusSeconds((long) jadeSources.get(source).getCooldown());
                return LocalDateTime.now().isBefore(cooldownEndTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }

        return false;
    }

    public long getCooldownTimeLeft(Player player, String source) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = this.getSQLConnection();
            String query = "SELECT timestamp FROM jade_transactions WHERE player = ? AND source = ? ORDER BY timestamp DESC LIMIT 1;";
            ps = conn.prepareStatement(query);
            ps.setString(1, player.getName().toLowerCase());
            ps.setString(2, source);
            rs = ps.executeQuery();

            if (rs.next()) {
                LocalDateTime lastTransactionTime = rs.getTimestamp("timestamp").toLocalDateTime();
                LocalDateTime cooldownEndTime = lastTransactionTime.plusSeconds((long) jadeSources.get(source).getCooldown());
                return ChronoUnit.SECONDS.between(LocalDateTime.now(), cooldownEndTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }

        return 0;
    }

    public HashMap<String, Double> getJadeFromSources(Player player) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        HashMap<String, Double> sourceJadeMap = new HashMap<>();

        try {
            conn = this.getSQLConnection();
            String query = "SELECT source, SUM(amount) AS total FROM jade_transactions WHERE player = ? AND amount > 0 AND timestamp >= ? GROUP BY source";

            // Check for transactions in the last 24 hours
            ps = conn.prepareStatement(query);
            ps.setString(1, player.getName().toLowerCase());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minus(24, ChronoUnit.HOURS)));
            rs = ps.executeQuery();

            while (rs.next()) {
                String source = rs.getString("source");
                double total = rs.getDouble("total");
                sourceJadeMap.put(source, total);
            }

            if (sourceJadeMap.isEmpty()) {
                rs.close();
                ps.close();

                ps = conn.prepareStatement(query);
                ps.setString(1, player.getName().toLowerCase());
                ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minus(30, ChronoUnit.DAYS)));
                rs = ps.executeQuery();

                while (rs.next()) {
                    String source = rs.getString("source");
                    double total = rs.getDouble("total");
                    sourceJadeMap.put(source, total);
                }

                if (sourceJadeMap.isEmpty()) {
                    sourceJadeMap.put("not_in_database", 0.0);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }

        return sourceJadeMap;
    }

    public void verifyAndFixTotals() {
        Connection conn = null;
        PreparedStatement psTransactions = null;
        PreparedStatement psTotals = null;
        ResultSet rsPlayers = null;

        try {
            conn = this.getSQLConnection();

            String queryPlayers = "SELECT player, jade FROM jade_totals;";
            psTotals = conn.prepareStatement(queryPlayers);
            rsPlayers = psTotals.executeQuery();

            while (rsPlayers.next()) {
                String player = rsPlayers.getString("player");
                int recordedTotal = rsPlayers.getInt("jade");

                String queryTransactions = "SELECT SUM(amount) AS total FROM jade_transactions WHERE player = ?;";
                psTransactions = conn.prepareStatement(queryTransactions);
                psTransactions.setString(1, player);
                ResultSet rsTransactions = psTransactions.executeQuery();

                int actualTotal = 0;
                if (rsTransactions.next()) {
                    actualTotal = rsTransactions.getInt("total");
                }
                rsTransactions.close();
                psTransactions.close();

                if (recordedTotal != actualTotal) {
                    plugin.getLogger().warning("Discrepancy found for player " + player + ": Recorded total = " + recordedTotal + ", Actual total = " + actualTotal);

                    String fixQuery = "UPDATE jade_totals SET jade = ? WHERE player = ?;";
                    PreparedStatement psFix = conn.prepareStatement(fixQuery);
                    psFix.setInt(1, actualTotal);
                    psFix.setString(2, player);
                    psFix.executeUpdate();
                    psFix.close();

                    plugin.getLogger().info("Fixed total for player " + player + ": Updated total = " + actualTotal);
                }
            }

            String queryMissingPlayers = "SELECT player, SUM(amount) AS total FROM jade_transactions WHERE player NOT IN (SELECT player FROM jade_totals) GROUP BY player;";
            psTransactions = conn.prepareStatement(queryMissingPlayers);
            ResultSet rsMissingPlayers = psTransactions.executeQuery();

            while (rsMissingPlayers.next()) {
                String player = rsMissingPlayers.getString("player");
                int total = rsMissingPlayers.getInt("total");

                String insertQuery = "INSERT INTO jade_totals (player, jade) VALUES (?, ?);";
                PreparedStatement psInsert = conn.prepareStatement(insertQuery);
                psInsert.setString(1, player);
                psInsert.setInt(2, total);
                psInsert.executeUpdate();
                psInsert.close();

                plugin.getLogger().info("Inserted new player " + player + " with total jade = " + total);
            }
            rsMissingPlayers.close();
            psTransactions.close();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rsPlayers != null) rsPlayers.close();
                if (psTransactions != null) psTransactions.close();
                if (psTotals != null) psTotals.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }
    }


    public void close(PreparedStatement ps, ResultSet rs) {
        try {
            if (ps != null) {
                ps.close();
            }

            if (rs != null) {
                rs.close();
            }
        } catch (SQLException var4) {
            Error.close(plugin, var4);
        }

    }

    public HashMap<String, Integer> getJadeLeaderboard() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        HashMap<String, Integer> leaderboard = new HashMap<>();

        try {
            conn = this.getSQLConnection();
            String query = "SELECT player, jade FROM jade_totals ORDER BY jade DESC LIMIT 10;";
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs.next()) {
                leaderboard.put(rs.getString("player"), rs.getInt("jade"));
            }
        } catch (SQLException var17) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), var17);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (ps != null) {
                    ps.close();
                }

                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException var16) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), var16);
            }

        }

        return leaderboard;
    }

    public List<String> getAllSources() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<String> sources = new ArrayList<>();

        try {
            conn = this.getSQLConnection();
            String query = "SELECT DISTINCT source FROM jade_transactions;";
            ps = conn.prepareStatement(query);
            rs = ps.executeQuery();

            while (rs.next()) {
                sources.add(rs.getString("source"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), e);
            }
        }

        return sources;
    }
}
