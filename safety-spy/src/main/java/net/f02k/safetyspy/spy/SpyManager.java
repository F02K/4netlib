package net.f02k.safetyspy.spy;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.config.SpyConfig;
import net.f02k.safetyspy.packet.PacketSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Enables / disables spy mode and keeps every client in sync with the
 * {@link VisibilityRegistry}. All methods must be called on the main thread
 * unless stated otherwise.
 */
public final class SpyManager {

    public static final String AUTOJOIN_PERMISSION = "safetyspy.autojoin";

    private final SafetySpyPlugin plugin;
    private final VisibilityRegistry registry;
    private final SpyStorage storage;
    private final Map<UUID, SpySession> sessions = new HashMap<>();

    public SpyManager(SafetySpyPlugin plugin, VisibilityRegistry registry, SpyStorage storage) {
        this.plugin = plugin;
        this.registry = registry;
        this.storage = storage;
    }

    private SpyConfig config() {
        return registry.config();
    }

    public boolean isSpy(Player player) {
        return registry.isSpy(player.getUniqueId());
    }

    public boolean canSee(Player viewer, Player spy) {
        return registry.canSee(viewer.getUniqueId(), spy.getUniqueId());
    }

    /** Online players currently in spy mode. */
    public List<Player> onlineSpies() {
        List<Player> spies = new ArrayList<>();
        for (UUID uuid : registry.spies()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                spies.add(player);
            }
        }
        return spies;
    }

    // ------------------------------------------------------------ toggling

    /** @return the new state */
    public boolean toggle(Player player) {
        if (isSpy(player)) {
            disable(player, true);
            return false;
        }
        enable(player, true);
        return true;
    }

    public void enable(Player player, boolean announce) {
        if (isSpy(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        mutate(() -> registry.addSpy(uuid, player.getName(), player.getEntityId()));
        sessions.put(uuid, SpySession.apply(player, config()));
        if (config().rememberState) {
            storage.set(uuid, true);
        }
        if (announce && config().fakeQuitOnEnable) {
            broadcastFake("fake-messages.quit", player);
        }
    }

    public void disable(Player player, boolean announce) {
        if (!isSpy(player)) {
            return;
        }
        UUID uuid = player.getUniqueId();
        // Before the change, so only players that could not see the spy get the message.
        if (announce && config().fakeJoinOnDisable) {
            broadcastFake("fake-messages.join", player);
        }
        mutate(() -> registry.removeSpy(uuid));
        SpySession session = sessions.remove(uuid);
        if (session != null) {
            session.restore(player);
        }
        storage.set(uuid, false);
    }

    // ---------------------------------------------------------- connection

    /** Async pre-login: hide remembered spies before any packet is sent. Thread-safe. */
    public void handlePreLogin(UUID uuid, String name) {
        if (config().rememberState && storage.contains(uuid)) {
            registry.addSpy(uuid, name, VisibilityRegistry.PENDING);
        }
    }

    public void handleJoin(Player player) {
        UUID uuid = player.getUniqueId();
        registry.setSeer(uuid, computeSeer(player));

        if (registry.isSpy(uuid)) {
            registry.updateEntityId(uuid, player.getName(), player.getEntityId());
        } else if (player.hasPermission(AUTOJOIN_PERMISSION)
                || (config().rememberState && storage.contains(uuid))) {
            mutate(() -> registry.addSpy(uuid, player.getName(), player.getEntityId()));
            if (config().rememberState) {
                storage.set(uuid, true);
            }
        } else {
            return;
        }
        sessions.put(uuid, SpySession.apply(player, config()));
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        SpySession session = sessions.remove(uuid);
        if (session != null) {
            session.restore(player);
        }
        registry.removeSpy(uuid);
        registry.setSeer(uuid, false);
    }

    /** Respawn / world change may give the player a new entity id. */
    public void refreshEntityId(Player player) {
        if (isSpy(player)) {
            registry.updateEntityId(player.getUniqueId(), player.getName(), player.getEntityId());
        }
    }

    // --------------------------------------------------------- visibility

    /** Re-checks the see permission of every online player. */
    public void refreshSeers() {
        mutate(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                registry.setSeer(player.getUniqueId(), computeSeer(player));
            }
        });
    }

    /** Applies a new config and re-sends every spy so the new filters apply. */
    public void applyConfig(SpyConfig newConfig) {
        for (Player spy : onlineSpies()) {
            SpySession old = sessions.remove(spy.getUniqueId());
            if (old != null) {
                old.restore(spy);
            }
        }
        registry.setConfig(newConfig);
        refreshSeers();
        for (Player spy : onlineSpies()) {
            sessions.put(spy.getUniqueId(), SpySession.apply(spy, newConfig));
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                resync(viewer, spy);
            }
        }
    }

    /**
     * Restores the player state of all spies (plugin disable). The stored spy
     * state is kept, so spies are hidden again on the next enable.
     */
    public void shutdown() {
        for (Player spy : onlineSpies()) {
            SpySession session = sessions.remove(spy.getUniqueId());
            if (session != null) {
                session.restore(spy);
            }
        }
    }

    private boolean computeSeer(Player player) {
        String permission = config().seePermission;
        return permission != null && !permission.isEmpty() && player.hasPermission(permission);
    }

    /**
     * Runs a state change and hides / shows spies for every viewer whose
     * visibility changed by it.
     */
    private void mutate(Runnable change) {
        Collection<? extends Player> online = List.copyOf(Bukkit.getOnlinePlayers());
        Map<UUID, Set<UUID>> before = new HashMap<>();
        for (Player viewer : online) {
            before.put(viewer.getUniqueId(), registry.hiddenFrom(viewer.getUniqueId()));
        }

        change.run();

        for (Player viewer : online) {
            Set<UUID> was = before.get(viewer.getUniqueId());
            Set<UUID> now = registry.hiddenFrom(viewer.getUniqueId());
            for (UUID spy : now) {
                if (!was.contains(spy)) {
                    hide(viewer, spy);
                }
            }
            for (UUID spy : was) {
                if (!now.contains(spy)) {
                    Player target = Bukkit.getPlayer(spy);
                    if (target != null) {
                        resync(viewer, target);
                    }
                }
            }
        }
    }

    private void hide(Player viewer, UUID spyUuid) {
        Player spy = Bukkit.getPlayer(spyUuid);
        if (spy == null) {
            return;
        }
        resync(viewer, spy);
        if (config().hideLocatorBar) {
            PacketSender.untrackWaypoint(viewer, spyUuid);
        }
    }

    /**
     * Lets the server remove and re-send the spy to the viewer. The outgoing
     * packets pass the PacketEvents filter, which decides what the viewer gets.
     */
    private void resync(Player viewer, Player spy) {
        if (viewer.equals(spy) || !viewer.canSee(spy)) {
            return;
        }
        viewer.hidePlayer(plugin, spy);
        viewer.showPlayer(plugin, spy);
    }

    private void broadcastFake(String path, Player spy) {
        if (plugin.messages().isEmpty(path)) {
            return;
        }
        Component message = plugin.messages().raw(path, Placeholder.component("player", spy.displayName()));
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (registry.isHiddenFrom(viewer.getUniqueId(), spy.getUniqueId())) {
                viewer.sendMessage(message);
            }
        }
        Bukkit.getConsoleSender().sendMessage(message);
    }
}
