package net.f02k.safetyspy.modules.safety;

import net.f02k.safetyspy.api.ModuleContext;
import net.f02k.safetyspy.api.SpyModule;

/**
 * Safety protection for spies (invulnerable, no mob target, no pickup,
 * no pressure plates, no sculk). All toggles live in config.yml -> safety.*.
 */
public final class SafetyModule implements SpyModule {

    @Override
    public void onEnable(ModuleContext context) {
        context.registerListener(new SafetyListener(context));
    }
}
