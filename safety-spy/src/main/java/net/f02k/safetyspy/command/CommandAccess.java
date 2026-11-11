package net.f02k.safetyspy.command;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Makes /spy exist only for players listed in {@code access.players}.
 * Everyone else - OPs included - gets "Unknown command" and never sees it.
 */
public final class CommandAccess {

    /** Command permission, granted only to listed players (controls /help visibility). */
    public static final String COMMAND_PERMISSION = "4netlib.command";

    private final SafetySpyPlugin plugin;
    private final Set<String> labels;
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public CommandAccess(SafetySpyPlugin plugin, PluginCommand command) {
        this.plugin = plugin;
        String namespace = plugin.getName().toLowerCase(Locale.ROOT) + ":";
        Set<String> all = new HashSet<>();
        all.add(command.getName().toLowerCase(Locale.ROOT));
        command.getAliases().forEach(alias -> all.add(alias.toLowerCase(Locale.ROOT)));
        for (String label : Set.copyOf(all)) {
            all.add(namespace + label);
        }
        this.labels = Collections.unmodifiableSet(all);
    }

    /** All labels of the command, incl. aliases and namespaced variants. */
    public Set<String> labels() {
        return labels;
    }

    public boolean isOwnLabel(String label) {
        return labels.contains(label.toLowerCase(Locale.ROOT));
    }

    /** Console always, players only when listed. */
    public boolean isAllowed(CommandSender sender) {
        return !(sender instanceof Player player)
                || plugin.spyConfig().hasAccess(player.getUniqueId(), player.getName());
    }

    /** Grants or revokes the command permission according to the access list. */
    public void apply(Player player) {
        boolean allowed = isAllowed(player);
        PermissionAttachment attachment = attachments.get(player.getUniqueId());
        if (allowed && attachment == null) {
            attachment = player.addAttachment(plugin);
            attachment.setPermission(COMMAND_PERMISSION, true);
            attachments.put(player.getUniqueId(), attachment);
        } else if (!allowed && attachment != null) {
            player.removeAttachment(attachment);
            attachments.remove(player.getUniqueId());
        }
    }

    public void forget(Player player) {
        attachments.remove(player.getUniqueId());
    }

    /** Re-applies the access list and re-sends the command tree (reload). */
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            apply(player);
            player.updateCommands();
        }
    }

    /** Vanilla style "Unknown command" answer. */
    public void sendUnknown(CommandSender sender, String input) {
        String context = input.length() > 10 ? "..." + input.substring(input.length() - 10) : input;
        sender.sendMessage(Component.translatable("command.unknown.command", NamedTextColor.RED));
        sender.sendMessage(Component.text(context, NamedTextColor.GRAY)
                .append(Component.translatable("command.context.here", NamedTextColor.RED)
                        .decorate(TextDecoration.ITALIC)));
    }
}
