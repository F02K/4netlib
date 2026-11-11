package net.f02k.safetyspy.spy;

import net.f02k.safetyspy.config.SpyConfig;
import org.bukkit.entity.Player;

/**
 * Player state changed by spy mode, restored when spy mode ends.
 */
final class SpySession {

    private final boolean collidable;
    private final boolean sleepingIgnored;
    private final boolean allowFlight;
    private final boolean flying;

    private SpySession(Player player) {
        this.collidable = player.isCollidable();
        this.sleepingIgnored = player.isSleepingIgnored();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
    }

    /** Saves the current state and applies the spy safety state. */
    static SpySession apply(Player player, SpyConfig config) {
        SpySession session = new SpySession(player);
        if (config.noCollision) {
            player.setCollidable(false);
        }
        if (config.ignoreSleep) {
            player.setSleepingIgnored(true);
        }
        if (config.allowFlight) {
            player.setAllowFlight(true);
        }
        return session;
    }

    void restore(Player player) {
        player.setCollidable(collidable);
        player.setSleepingIgnored(sleepingIgnored);
        player.setAllowFlight(allowFlight);
        if (allowFlight) {
            player.setFlying(flying && player.isFlying());
        }
    }
}
