package net.f02k.safetyspy.listener;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.spy.SpyManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Join / quit handling, message suppression and entity id tracking.
 */
public final class ConnectionListener implements Listener {

    private final SafetySpyPlugin plugin;
    private final SpyManager manager;

    public ConnectionListener(SafetySpyPlugin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            manager.handlePreLogin(event.getUniqueId(), event.getName());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinEarly(PlayerJoinEvent event) {
        manager.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinMessage(PlayerJoinEvent event) {
        if (plugin.spyConfig().hideJoinQuitMessages && manager.isSpy(event.getPlayer())) {
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitMessage(PlayerQuitEvent event) {
        if (plugin.spyConfig().hideJoinQuitMessages && manager.isSpy(event.getPlayer())) {
            event.quitMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        manager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        if (plugin.spyConfig().hideAdvancementMessages && manager.isSpy(event.getPlayer())) {
            event.message(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) {
                manager.refreshEntityId(event.getPlayer());
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        manager.refreshEntityId(event.getPlayer());
    }
}
