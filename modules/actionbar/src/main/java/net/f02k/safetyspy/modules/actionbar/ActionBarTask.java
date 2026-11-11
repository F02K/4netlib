package net.f02k.safetyspy.modules.actionbar;

import net.f02k.safetyspy.api.ModuleContext;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Reminds spies that they are in spy mode.
 */
public final class ActionBarTask implements Runnable {

    private final ModuleContext context;

    public ActionBarTask(ModuleContext context) {
        this.context = context;
    }

    @Override
    public void run() {
        if (!context.config().actionBarEnabled || context.messages().isEmpty("action-bar.message")) {
            return;
        }
        Component message = context.messages().raw("action-bar.message");
        for (Player spy : context.spyManager().onlineSpies()) {
            spy.sendActionBar(message);
        }
    }
}
