package net.f02k.safetyspy.modules.chat;

import net.f02k.safetyspy.api.ModuleContext;
import net.f02k.safetyspy.api.SpyModule;

/**
 * Keeps a spy's public chat from revealing them: BLOCK, SEERS or PUBLIC
 * (config.yml -> chat.mode).
 */
public final class ChatModule implements SpyModule {

    @Override
    public void onEnable(ModuleContext context) {
        context.registerListener(new ChatListener(context));
    }
}
