package net.f02k.safetyspy.task;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.spy.SpyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Reminds spies that they are in spy mode.
 */
public final class ActionBarTask implements Runnable {

    private final SafetySpyPlugin plugin;
    private final SpyManager manager;

    public ActionBarTask(SafetySpyPlugin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void run() {
        if (!plugin.spyConfig().actionBarEnabled || plugin.messages().isEmpty("action-bar.message")) {
            return;
        }
        Component message = plugin.messages().raw("action-bar.message");
        for (Player spy : manager.onlineSpies()) {
            spy.sendActionBar(message);
        }
    }
}
