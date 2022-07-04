package ru.mops1k.sqlplayerlist;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.bukkit.configuration.Configuration;

/**
 * Created by OsipXD on 22.09.2016
 * It is part of the Plugin.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
class SSHTunnel {
    private final String host;
    private final int port;
    private final String remoteHost;
    private final int remotePort;
    private final String username;
    private final String password;
    private final int localPort;
    private Session session = null;

    SSHTunnel(Configuration config, String sqlHost, int sqlPort) throws JSchException {
        Plugin.getInstance().getLogger().info("Configuring SSH Tunnel...");
        this.host = config.getString("tunnel.host");
        this.port = config.getInt("tunnel.port.ssh");
        this.username = config.getString("tunnel.user");
        this.password = config.getString("tunnel.pass");
        this.localPort = config.getInt("tunnel.port.local");
        this.remoteHost = sqlHost;
        this.remotePort = sqlPort;

        this.connect();
        Plugin.getInstance().getLogger().info("Test SSH connection was successful!");
        this.disconnect();
    }

    int getLocalPort() {
        return localPort;
    }

    void connect() throws JSchException {
        if (this.session == null || !this.session.isConnected()) {
            final JSch jsch = new JSch();
            this.session = jsch.getSession(username, host, port);
            this.session.setPassword(password);
            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.connect();
            this.session.setPortForwardingL(localPort, remoteHost, remotePort);
        }
    }

    void disconnect() {
        if (this.session != null && this.session.isConnected()) {
            this.session.disconnect();
        }
    }
}
