package net.f02k.safetyspy.modules.actionbar;

import net.f02k.safetyspy.api.ModuleContext;
import net.f02k.safetyspy.api.SpyModule;

/**
 * Shows a configurable action-bar reminder to players in spy mode.
 */
public final class ActionBarModule implements SpyModule {

    @Override
    public void onEnable(ModuleContext context) {
        context.runTimer(new ActionBarTask(context), 20L, 20L);
    }
}
