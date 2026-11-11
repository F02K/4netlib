package net.f02k.safetyspy.api;

/**
 * Entry point of a 4netlib module. A module is a separate jar placed in
 * {@code plugins/4netlib/modules/}. Its jar must contain a {@code module.yml}
 * with {@code name} and {@code main} (the fully qualified name of the class
 * implementing this interface and having a public no-arg constructor).
 *
 * <p>Modules are loaded after the core is ready, so {@link ModuleContext}
 * already exposes a working spy manager and registry.
 */
public interface SpyModule {

    /** Called when the module is enabled. Register listeners/tasks via {@code context}. */
    void onEnable(ModuleContext context);

    /** Called when the module is disabled. Listeners/tasks registered through
     * the context are removed automatically; override only for extra cleanup. */
    default void onDisable() {
    }
}
