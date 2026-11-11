package net.f02k.safetyspy.module;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.api.ModuleContext;
import net.f02k.safetyspy.config.Messages;
import net.f02k.safetyspy.config.SpyConfig;
import net.f02k.safetyspy.spy.SpyManager;
import net.f02k.safetyspy.spy.VisibilityRegistry;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@link ModuleContext} backed by the core plugin. Records every registration
 * so {@link #cleanup()} can undo it when the module is disabled.
 */
final class ModuleContextImpl implements ModuleContext {

    private final SafetySpyPlugin plugin;
    private final Logger logger;
    private final List<Listener> listeners = new ArrayList<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<PacketListenerCommon> packetListeners = new ArrayList<>();

    ModuleContextImpl(SafetySpyPlugin plugin, String moduleName) {
        this.plugin = plugin;
        this.logger = Logger.getLogger(plugin.getLogger().getName() + "/" + moduleName);
    }

    @Override
    public JavaPlugin plugin() {
        return plugin;
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public SpyManager spyManager() {
        return plugin.manager();
    }

    @Override
    public VisibilityRegistry registry() {
        return plugin.registry();
    }

    @Override
    public SpyConfig config() {
        return plugin.spyConfig();
    }

    @Override
    public Messages messages() {
        return plugin.messages();
    }

    @Override
    public void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }

    @Override
    public BukkitTask runTimer(Runnable task, long delayTicks, long periodTicks) {
        BukkitTask handle = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, task, delayTicks, periodTicks);
        tasks.add(handle);
        return handle;
    }

    @Override
    public PacketListenerCommon registerPacketListener(PacketListener listener, PacketListenerPriority priority) {
        PacketListenerCommon handle = PacketEvents.getAPI().getEventManager()
                .registerListener(listener, priority);
        packetListeners.add(handle);
        return handle;
    }

    /** Removes everything this module registered. */
    void cleanup() {
        listeners.forEach(HandlerList::unregisterAll);
        listeners.clear();
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
        packetListeners.forEach(PacketEvents.getAPI().getEventManager()::unregisterListener);
        packetListeners.clear();
    }
}
