package net.f02k.safetyspy.spy;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persists which players are in spy mode (data.yml).
 */
public final class SpyStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> stored = ConcurrentHashMap.newKeySet();

    public SpyStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        stored.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String raw : yaml.getStringList("spies")) {
            try {
                stored.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Invalid UUID in data.yml: " + raw);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("spies", stored.stream().map(UUID::toString).sorted().toList());
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save data.yml", e);
        }
    }

    public boolean contains(UUID uuid) {
        return stored.contains(uuid);
    }

    public void set(UUID uuid, boolean spy) {
        boolean changed = spy ? stored.add(uuid) : stored.remove(uuid);
        if (changed) {
            save();
        }
    }
}
