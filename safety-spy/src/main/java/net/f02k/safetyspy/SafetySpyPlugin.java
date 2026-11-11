package net.f02k.safetyspy;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import net.f02k.safetyspy.command.CommandAccess;
import net.f02k.safetyspy.command.SpyCommand;
import net.f02k.safetyspy.config.Messages;
import net.f02k.safetyspy.config.SpyConfig;
import net.f02k.safetyspy.listener.ChatListener;
import net.f02k.safetyspy.listener.CommandAccessListener;
import net.f02k.safetyspy.listener.CommandListener;
import net.f02k.safetyspy.listener.ConnectionListener;
import net.f02k.safetyspy.listener.SafetyListener;
import net.f02k.safetyspy.packet.SpyPacketListener;
import net.f02k.safetyspy.spy.SpyManager;
import net.f02k.safetyspy.spy.SpyStorage;
import net.f02k.safetyspy.spy.VisibilityRegistry;
import net.f02k.safetyspy.task.ActionBarTask;
import net.f02k.safetyspy.task.SeerRefreshTask;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class SafetySpyPlugin extends JavaPlugin {

    private volatile SpyConfig spyConfig;
    private volatile Messages messages;
    private SpyManager manager;
    private CommandAccess access;
    private PacketListenerCommon packetListener;
    private final List<BukkitTask> tasks = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        spyConfig = new SpyConfig(getConfig());
        messages = new Messages(getConfig());

        SpyStorage storage = new SpyStorage(this);
        storage.load();
        VisibilityRegistry registry = new VisibilityRegistry(spyConfig);
        manager = new SpyManager(this, registry, storage);

        packetListener = PacketEvents.getAPI().getEventManager()
                .registerListener(new SpyPacketListener(registry), PacketListenerPriority.HIGH);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ConnectionListener(this, manager), this);
        pm.registerEvents(new SafetyListener(this, manager), this);
        pm.registerEvents(new ChatListener(this, manager), this);
        pm.registerEvents(new CommandListener(this, manager), this);

        PluginCommand command = getCommand("spy");
        if (command != null) {
            access = new CommandAccess(this, command);
            pm.registerEvents(new CommandAccessListener(access), this);
            SpyCommand executor = new SpyCommand(this, manager, access);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
            access.refreshAll();
        }

        // Plugin reload while players are online.
        for (Player player : Bukkit.getOnlinePlayers()) {
            manager.handleJoin(player);
        }
        startTasks();
    }

    @Override
    public void onDisable() {
        stopTasks();
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
        }
        if (manager != null) {
            manager.shutdown();
        }
    }

    /** Reloads config.yml and re-applies it to all spies. */
    public void reload() {
        reloadConfig();
        spyConfig = new SpyConfig(getConfig());
        messages = new Messages(getConfig());
        manager.applyConfig(spyConfig);
        if (access != null) {
            access.refreshAll();
        }
        stopTasks();
        startTasks();
    }

    private void startTasks() {
        tasks.add(Bukkit.getScheduler().runTaskTimer(this, new ActionBarTask(this, manager), 20L, 20L));
        long interval = spyConfig.refreshIntervalTicks;
        tasks.add(Bukkit.getScheduler().runTaskTimer(this, new SeerRefreshTask(manager), interval, interval));
    }

    private void stopTasks() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    public SpyConfig spyConfig() {
        return spyConfig;
    }

    public Messages messages() {
        return messages;
    }
}
