package ru.mops1k.sqlplayerlist;

import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the Plugin.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
public class Plugin extends JavaPlugin {
    private static Plugin instance;

    public SQL sql;

    static Plugin getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();

        if (!this.getConfig().getBoolean("enabled")) {
            getPluginLoader().disablePlugin(this);
            return;
        }

        this.sql = new SQL();
        this.initSQL();

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(), this);

        if (this.sql.isKilled()) {
            pm.disablePlugin(this);
        }
    }

    public void onDisable() {
        if (this.sql == null) {
            return;
        }

        if (this.sql.isKilled()) {
            this.getLogger().warning("Please configure your SQL connection");
        } else {
            this.getLogger().info("Setting status to offline...");
        }
    }

    private void initSQL() {
        if (!this.sql.tableExists() && !this.sql.isKilled()) {
            this.sql.createTable();

            if (this.sql.isKilled()) {
                this.getLogger().warning("Table not created!");
            }
        }
    }
}