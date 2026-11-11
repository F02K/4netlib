package net.f02k.safetyspy.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.config.ChatMode;
import net.f02k.safetyspy.spy.SpyManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Public chat of spies. Hidden viewers never receive a signed chat packet from
 * a sender they do not know, which would otherwise show a verification error.
 */
public final class ChatListener implements Listener {

    private final SafetySpyPlugin plugin;
    private final SpyManager manager;

    public ChatListener(SafetySpyPlugin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player spy = event.getPlayer();
        if (!manager.isSpy(spy)) {
            return;
        }
        ChatMode mode = plugin.spyConfig().chatMode;
        if (mode == ChatMode.BLOCK) {
            event.setCancelled(true);
            plugin.messages().send(spy, "chat-blocked");
            return;
        }

        List<Player> hiddenViewers = new ArrayList<>();
        event.viewers().removeIf(audience -> {
            if (audience instanceof Player viewer && !manager.canSee(viewer, spy)) {
                hiddenViewers.add(viewer);
                return true;
            }
            return false;
        });

        if (mode == ChatMode.PUBLIC) {
            for (Player viewer : hiddenViewers) {
                Component rendered = event.renderer().render(spy, spy.displayName(), event.message(), viewer);
                viewer.sendMessage(rendered);
            }
        }
    }
}
