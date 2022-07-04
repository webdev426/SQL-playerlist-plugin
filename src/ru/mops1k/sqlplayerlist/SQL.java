package ru.mops1k.sqlplayerlist;

import com.jcraft.jsch.JSchException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.text.SimpleDateFormat;

class SQL {
    private final Thread executor = new Thread(new SQLExecutor());

    private final String url;
    private final String username;
    private final String password;
    private final String table;
    private final String server;

    private FileConfiguration config;
    private boolean killed;
    private String query;

    private SSHTunnel tunnel;
    private Connection connection;

    public SQL() {
        this.reloadConfig();

        String host = this.config.getString("sql.host");
        String db = this.config.getString("sql.db");
        int port = this.config.getInt("sql.port");

        this.username = this.config.getString("sql.user");
        this.password = this.config.getString("sql.pass");
        this.table = this.config.getString("sql.table");
        this.server = this.config.getString("server-name");

        if (this.config.getBoolean("tunnel.enabled")) {
            try {
                this.tunnel = new SSHTunnel(this.config, host, port);
            } catch (JSchException e) {
                this.url = null;
                Plugin.getInstance().getLogger().severe("Error when configuring SSH tunnel: " + e);
                this.killed = true;
                return;
            }

            this.url = "jdbc:mysql://localhost:" + this.tunnel.getLocalPort() + "/" + db;
        } else {
            this.url = "jdbc:mysql://" + host + ":" + port + "/" + db;
            this.tunnel = null;
        }

        try {
            this.connect();
            Plugin.getInstance().getLogger().info("Test SQL connection was successful!");
            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            Plugin.getInstance().getLogger().severe("Test SQL connection failed: " + e);
            e.printStackTrace();
            return;
        }

        this.killed = false;
    }

    private void connect() throws SQLException, JSchException {
        if (this.tunnel != null) {
            this.tunnel.connect();
        }

        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }

        this.connection = DriverManager.getConnection(this.url, this.username, this.password);
    }

    private void disconnect() throws SQLException {
        if (this.tunnel != null) {
            this.tunnel.disconnect();
        }

        if (this.connection != null && !this.connection.isClosed()) {
            this.connection.close();
        }
    }

    private void executeQuery() {
        if (this.killed) {
            return;
        }

        if (this.executor.isAlive()) {
            try {
                this.executor.join();
            } catch (InterruptedException ignored) {
            }
        }

        this.executor.run();
    }

    private void reloadConfig() {
        this.config = Plugin.getInstance().getConfig();
    }

    void updatePlayer(Player player, int status, java.util.Date timePoint) {
        if (!this.playerExists(player)) {
            this.addPlayer(player);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sqlDate = dateFormat.format(timePoint);

        this.query = "UPDATE " + this.table + " SET \n" +
                " status = " + status + ", \n";

        switch (status) {
            case 0:
                ResultSet rs = this.playerData(player);
                try {
                    Timestamp last_login = null;
                    if (rs != null) {
                        last_login = rs.getTimestamp("last_login");
                        java.util.Date last_date = new java.util.Date(last_login.getTime());

                        long total_time = timePoint.getTime() - last_date.getTime();
                        Plugin.getInstance().getLogger().info("Player '" + player.getName() + "' was online " + total_time/1000 + " seconds");
                        this.query += " total_time = total_time + " + total_time/1000 + ", \n";
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.query += " last_logout = '" + sqlDate + "' \n";
                break;
            case 1:
                this.query += " last_login = '" + sqlDate + "' \n";
                break;
            default:
                return;
        }

        this.query += "WHERE server = '" + this.server + "' AND player = '" + player.getName() + "'";
        this.executeQuery();
    }

    boolean tableExists() {
        Plugin.getInstance().getLogger().info("Checking the existence of the table...");
        try {
            this.connect();
            DatabaseMetaData meta = this.connection.getMetaData();
            ResultSet rs = meta.getTables(null, null, this.table, null);

            if (rs.next()) {
                return true;
            }

            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            Plugin.getInstance().getLogger().severe("Connection to DB failed: " + e);
        }

        return false;
    }

    @Nullable
    private ResultSet playerData(Player player) {
        try {
            this.connect();
            PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM " + this.table +
                    " WHERE server = '" + this.server + "' AND player = '" + player.getName() + "'");
            ResultSet rs = statement.executeQuery();
            rs.next();

            return rs;
        } catch (SQLException | JSchException e) {
            this.killed = true;
            e.printStackTrace();
        }
        return null;
    }

    private boolean playerExists(Player player) {
        try {
            this.connect();
            PreparedStatement statement = this.connection.prepareStatement("SELECT 1 FROM " + this.table +
                    " WHERE server = '" + this.server + "' AND player = '" + player.getName() + "'");
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return true;
            }

            this.disconnect();
        } catch (SQLException | JSchException e) {
            this.killed = true;
            e.printStackTrace();
        }

        return false;
    }

    void createTable() {
        Plugin.getInstance().getLogger().info("Creating table...");
        this.query = "CREATE TABLE `" + this.table + "` (\n" +
                "`server` VARCHAR(50) NULL,\n" +
                "`player` VARCHAR(50) NULL,\n" +
                "`status` SMALLINT NULL DEFAULT NULL,\n" +
                "`total_time` INT NULL DEFAULT '0',\n" +
                "`last_login` DATETIME NULL DEFAULT NULL,\n" +
                "`last_logout` DATETIME NULL DEFAULT NULL\n" +
                ")";
        this.executeQuery();
    }

    private void addPlayer(Player player) {
        Plugin.getInstance().getLogger().info("Adding player to table...");
        this.query = "INSERT INTO " + this.table + " (player, server)\n" +
                "  VALUE ('" + player.getName() + "', '" + this.server + "')";
        this.executeQuery();

        try {
            this.executor.join();
        } catch (InterruptedException ignored) {
        }

        if (this.killed) {
            Plugin.getInstance().getLogger().warning("Failed to add player to DB!");
        }
    }

    boolean isKilled() {
        return this.killed;
    }

    private class SQLExecutor implements Runnable {
        @Override
        public void run() {
            try {
                SQL.this.connect();
                PreparedStatement statement = SQL.this.connection.prepareStatement(SQL.this.query);
                statement.executeUpdate();
                SQL.this.disconnect();
            } catch (SQLException | JSchException e) {
                Plugin.getInstance().getLogger().severe("Connection to DB failed: " + e);
                SQL.this.killed = true;
            }
        }
    }
}
