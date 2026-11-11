package net.f02k.safetyspy.api;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import net.f02k.safetyspy.config.Messages;
import net.f02k.safetyspy.config.SpyConfig;
import net.f02k.safetyspy.spy.SpyManager;
import net.f02k.safetyspy.spy.VisibilityRegistry;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.logging.Logger;

/**
 * Everything a {@link SpyModule} needs from the core. Anything registered
 * through this context is tracked and removed when the module is disabled.
 */
public interface ModuleContext {

    /** The core plugin instance (scheduler, data folder, ...). */
    JavaPlugin plugin();

    /** Logger named after the module. */
    Logger logger();

    /** Enables / disables spy mode and answers visibility questions. */
    SpyManager spyManager();

    /** Low level, thread-safe visibility state (for packet modules). */
    VisibilityRegistry registry();

    /** Current config snapshot. Always returns the latest after a reload. */
    SpyConfig config();

    /** Current messages. Always returns the latest after a reload. */
    Messages messages();

    /** Registers a Bukkit listener, unregistered automatically on disable. */
    void registerListener(Listener listener);

    /** Registers a repeating task, cancelled automatically on disable. */
    BukkitTask runTimer(Runnable task, long delayTicks, long periodTicks);

    /** Registers a PacketEvents listener, unregistered automatically on disable. */
    PacketListenerCommon registerPacketListener(PacketListener listener, PacketListenerPriority priority);
}
