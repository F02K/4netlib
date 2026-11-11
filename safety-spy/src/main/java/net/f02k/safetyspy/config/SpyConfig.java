package net.f02k.safetyspy.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of config.yml. Safe to read from netty threads.
 */
public final class SpyConfig {

    public final boolean hideEntity;
    public final boolean hideTablist;
    public final boolean keepProfile;
    public final boolean hideServerList;
    public final boolean hideTabComplete;
    public final boolean hideLocatorBar;
    public final boolean hideJoinQuitMessages;
    public final boolean hideAdvancementMessages;
    public final boolean hideListCommand;
    public final Set<String> playerCommands;

    public final String seePermission;
    public final boolean spiesSeeEachOther;
    public final long refreshIntervalTicks;

    public final ChatMode chatMode;

    public final boolean fakeQuitOnEnable;
    public final boolean fakeJoinOnDisable;

    public final boolean invulnerable;
    public final boolean noMobTarget;
    public final boolean noItemPickup;
    public final boolean noPhysicalInteract;
    public final boolean noSculk;
    public final boolean noCollision;
    public final boolean ignoreSleep;
    public final boolean allowFlight;

    public final boolean rememberState;

    public final boolean actionBarEnabled;

    public SpyConfig(FileConfiguration c) {
        hideEntity = c.getBoolean("hide.entity", true);
        hideTablist = c.getBoolean("hide.tablist", true);
        keepProfile = c.getBoolean("hide.keep-profile", false);
        hideServerList = c.getBoolean("hide.server-list", true);
        hideTabComplete = c.getBoolean("hide.tab-complete", true);
        hideLocatorBar = c.getBoolean("hide.locator-bar", true);
        hideJoinQuitMessages = c.getBoolean("hide.join-quit-messages", true);
        hideAdvancementMessages = c.getBoolean("hide.advancement-messages", true);
        hideListCommand = c.getBoolean("hide.list-command", true);
        playerCommands = c.getStringList("hide.player-commands").stream()
                .map(command -> command.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        seePermission = c.getString("visibility.see-permission", "safetyspy.see");
        spiesSeeEachOther = c.getBoolean("visibility.spies-see-each-other", true);
        refreshIntervalTicks = Math.max(1L, c.getLong("visibility.refresh-interval-ticks", 40L));

        chatMode = ChatMode.parse(c.getString("chat.mode", "BLOCK"));

        fakeQuitOnEnable = c.getBoolean("fake-messages.quit-on-enable", true);
        fakeJoinOnDisable = c.getBoolean("fake-messages.join-on-disable", true);

        invulnerable = c.getBoolean("safety.invulnerable", true);
        noMobTarget = c.getBoolean("safety.no-mob-target", true);
        noItemPickup = c.getBoolean("safety.no-item-pickup", true);
        noPhysicalInteract = c.getBoolean("safety.no-physical-interact", true);
        noSculk = c.getBoolean("safety.no-sculk", true);
        noCollision = c.getBoolean("safety.no-collision", true);
        ignoreSleep = c.getBoolean("safety.ignore-sleep", true);
        allowFlight = c.getBoolean("safety.allow-flight", true);

        rememberState = c.getBoolean("persistence.remember-state", true);

        actionBarEnabled = c.getBoolean("action-bar.enabled", true);
    }

    /** Remove the spy's player info entry completely from hidden viewers. */
    public boolean removeProfile() {
        return hideTablist && hideEntity && !keepProfile;
    }

    /** Keep the player info entry but mark it as not listed. */
    public boolean unlistProfile() {
        return hideTablist && !removeProfile();
    }
}
