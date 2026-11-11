package net.f02k.safetyspy.listener;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.config.SpyConfig;
import net.f02k.safetyspy.spy.SpyManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Keeps commands from revealing spies to players that cannot see them.
 */
public final class CommandListener implements Listener {

    private final SafetySpyPlugin plugin;
    private final SpyManager manager;

    public CommandListener(SafetySpyPlugin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player sender = event.getPlayer();
        String[] parts = event.getMessage().substring(1).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return;
        }
        String label = parts[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        SpyConfig config = plugin.spyConfig();

        if (config.hideListCommand && label.equals("list") && parts.length == 1) {
            event.setCancelled(true);
            sendList(sender);
            return;
        }

        if (!config.playerCommands.contains(label)) {
            return;
        }
        for (int i = 1; i < parts.length; i++) {
            Player target = Bukkit.getPlayerExact(parts[i]);
            if (target != null && manager.isSpy(target) && !manager.canSee(sender, target)) {
                event.setCancelled(true);
                sender.sendMessage(Component.translatable("argument.entity.notfound.player", NamedTextColor.RED));
                return;
            }
        }
    }

    /** Vanilla style /list without hidden spies. */
    private void sendList(Player viewer) {
        List<Component> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!manager.isSpy(player) || manager.canSee(viewer, player)) {
                names.add(player.displayName());
            }
        }
        viewer.sendMessage(Component.translatable("commands.list.players",
                Component.text(names.size()),
                Component.text(Bukkit.getMaxPlayers()),
                Component.join(JoinConfiguration.commas(true), names)));
    }
}
