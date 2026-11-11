package net.f02k.safetyspy.command;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.config.Messages;
import net.f02k.safetyspy.spy.SpyManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /spy [player|list|reload]
 */
public final class SpyCommand implements TabExecutor {

    private static final String PERM_USE = "safetyspy.use";
    private static final String PERM_OTHERS = "safetyspy.others";
    private static final String PERM_LIST = "safetyspy.list";
    private static final String PERM_RELOAD = "safetyspy.reload";

    private final SafetySpyPlugin plugin;
    private final SpyManager manager;

    public SpyCommand(SafetySpyPlugin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.messages();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!player.hasPermission(PERM_USE)) {
                messages.send(sender, "no-permission");
                return true;
            }
            messages.send(player, manager.toggle(player) ? "enabled" : "disabled");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender, messages);
            case "reload" -> {
                if (!sender.hasPermission(PERM_RELOAD)) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                plugin.reload();
                plugin.messages().send(sender, "reloaded");
            }
            default -> toggleOther(sender, args[0], messages);
        }
        return true;
    }

    private void list(CommandSender sender, Messages messages) {
        if (!sender.hasPermission(PERM_LIST)) {
            messages.send(sender, "no-permission");
            return;
        }
        List<Player> spies = manager.onlineSpies();
        if (spies.isEmpty()) {
            messages.send(sender, "list-empty");
            return;
        }
        messages.send(sender, "list-header", Placeholder.unparsed("count", String.valueOf(spies.size())));
        for (Player spy : spies) {
            messages.send(sender, "list-entry", Placeholder.unparsed("player", spy.getName()));
        }
    }

    private void toggleOther(CommandSender sender, String name, Messages messages) {
        if (!sender.hasPermission(PERM_OTHERS)) {
            messages.send(sender, "no-permission");
            return;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            messages.send(sender, "player-not-found", Placeholder.unparsed("player", name));
            return;
        }
        boolean enabled = manager.toggle(target);
        messages.send(target, enabled ? "enabled" : "disabled");
        if (!target.equals(sender)) {
            messages.send(sender, enabled ? "enabled-other" : "disabled-other",
                    Placeholder.unparsed("player", target.getName()));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if (sender.hasPermission(PERM_LIST)) {
            result.add("list");
        }
        if (sender.hasPermission(PERM_RELOAD)) {
            result.add("reload");
        }
        if (sender.hasPermission(PERM_OTHERS)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!(sender instanceof Player viewer) || viewer.canSee(player) && canSee(viewer, player)) {
                    result.add(player.getName());
                }
            }
        }
        result.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(prefix));
        return result;
    }

    private boolean canSee(Player viewer, Player target) {
        return !manager.isSpy(target) || manager.canSee(viewer, target);
    }
}
