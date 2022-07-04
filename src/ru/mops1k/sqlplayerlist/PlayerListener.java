package ru.mops1k.sqlplayerlist;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by OsipXD on 13.09.2015
 * It is part of the Plugin.
 * All rights reserved 2014 - 2016 © «EndlessCode Group»
 */
class PlayerListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Plugin.getInstance().sql.updatePlayer(event.getPlayer(), 1, new Date());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Plugin.getInstance().sql.updatePlayer(event.getPlayer(), 0, new Date());
    }
}
