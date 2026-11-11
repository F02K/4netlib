package net.f02k.safetyspy.module;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.api.SpyModule;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * Loads module jars from {@code plugins/4netlib/modules/} and manages their
 * life cycle. Each module gets its own class loader whose parent is the core
 * plugin class loader, so modules see Paper, PacketEvents and the core API.
 */
public final class ModuleManager {

    private final SafetySpyPlugin plugin;
    private final File folder;
    private final List<LoadedModule> loaded = new ArrayList<>();

    public ModuleManager(SafetySpyPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "modules");
    }

    /** Scans the folder and enables every module jar found. */
    public void enableAll() {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create modules folder: " + folder);
            return;
        }
        File[] jars = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            return;
        }
        Arrays.sort(jars);
        for (File jar : jars) {
            enable(jar);
        }
        if (!loaded.isEmpty()) {
            plugin.getLogger().info("Enabled modules: " + String.join(", ", moduleNames()));
        }
    }

    private void enable(File jar) {
        LoadedModule module = null;
        try {
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{jar.toURI().toURL()}, plugin.getClass().getClassLoader());
            ModuleDescriptor descriptor = ModuleDescriptor.read(loader);
            Class<?> type = Class.forName(descriptor.main(), true, loader);
            SpyModule instance = type.asSubclass(SpyModule.class).getDeclaredConstructor().newInstance();
            ModuleContextImpl context = new ModuleContextImpl(plugin, descriptor.name());
            module = new LoadedModule(descriptor, instance, loader, context);
            instance.onEnable(context);
            loaded.add(module);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable module " + jar.getName(), t);
            if (module != null) {
                module.context().cleanup();
                module.closeQuietly();
            }
        }
    }

    /** Disables every module in reverse order and releases its class loader. */
    public void disableAll() {
        for (int i = loaded.size() - 1; i >= 0; i--) {
            LoadedModule module = loaded.get(i);
            try {
                module.module().onDisable();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE,
                        "Error disabling module '" + module.descriptor().name() + "'", t);
            }
            module.context().cleanup();
            module.closeQuietly();
        }
        loaded.clear();
    }

    /** Re-scans the folder (picks up added / removed module jars). */
    public void reload() {
        disableAll();
        enableAll();
    }

    public List<String> moduleNames() {
        List<String> names = new ArrayList<>();
        for (LoadedModule module : loaded) {
            names.add(module.descriptor().name());
        }
        return Collections.unmodifiableList(names);
    }
}
