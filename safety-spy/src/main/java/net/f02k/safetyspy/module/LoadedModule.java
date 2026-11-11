package net.f02k.safetyspy.module;

import net.f02k.safetyspy.api.SpyModule;

import java.io.IOException;
import java.net.URLClassLoader;

/**
 * A module jar after it has been loaded: descriptor, instance, class loader and
 * the context used to register its listeners / tasks.
 */
record LoadedModule(ModuleDescriptor descriptor, SpyModule module,
                    URLClassLoader loader, ModuleContextImpl context) {

    void closeQuietly() {
        try {
            loader.close();
        } catch (IOException ignored) {
            // nothing we can do about a leaking module class loader
        }
    }
}
