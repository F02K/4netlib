package net.f02k.safetyspy.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * MiniMessage based message access for config.yml.
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final FileConfiguration config;

    public Messages(FileConfiguration config) {
        this.config = config;
    }

    /** Message from the {@code messages} section, prefixed. */
    public void send(CommandSender to, String key, TagResolver... resolvers) {
        String raw = config.getString("messages." + key, "");
        if (raw.isEmpty()) {
            return;
        }
        to.sendMessage(MM.deserialize(config.getString("messages.prefix", "") + raw, resolvers));
    }

    /** Any path of the config, without prefix. */
    public Component raw(String path, TagResolver... resolvers) {
        return MM.deserialize(config.getString(path, ""), resolvers);
    }

    public boolean isEmpty(String path) {
        return config.getString(path, "").isEmpty();
    }
}
