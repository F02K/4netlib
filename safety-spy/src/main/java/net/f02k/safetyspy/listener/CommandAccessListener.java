package net.f02k.safetyspy.listener;

import net.f02k.safetyspy.command.CommandAccess;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Hides /spy from every player that is not in the access list.
 */
public final class CommandAccessListener implements Listener {

    private final CommandAccess access;

    public CommandAccessListener(CommandAccess access) {
        this.access = access;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        access.apply(player);
        player.updateCommands();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        access.forget(event.getPlayer());
    }

    /** Removes the command from the client command tree (tab-complete). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandSend(PlayerCommandSendEvent event) {
        if (!access.isAllowed(event.getPlayer())) {
            event.getCommands().removeAll(access.labels());
        }
    }

    /** Answers like a non-existing command, before any other plugin sees it. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String input = event.getMessage().substring(1);
        String label = input.split(" ", 2)[0];
        if (access.isOwnLabel(label) && !access.isAllowed(event.getPlayer())) {
            event.setCancelled(true);
            access.sendUnknown(event.getPlayer(), input);
        }
    }
}
