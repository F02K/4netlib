package net.f02k.safetyspy.spy;

import net.f02k.safetyspy.config.SpyConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe visibility state. Written on the main thread, read by the
 * PacketEvents listeners on netty threads.
 */
public final class VisibilityRegistry {

    /** Entity id of a spy that has not fully joined yet. */
    public static final int PENDING = -1;

    private final Map<UUID, Integer> spyEntityIds = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> entityToSpy = new ConcurrentHashMap<>();
    private final Map<UUID, String> spyNames = new ConcurrentHashMap<>();
    private final Set<UUID> seers = ConcurrentHashMap.newKeySet();

    private volatile SpyConfig config;

    public VisibilityRegistry(SpyConfig config) {
        this.config = config;
    }

    public SpyConfig config() {
        return config;
    }

    public void setConfig(SpyConfig config) {
        this.config = config;
    }

    // ---------------------------------------------------------------- spies

    public void addSpy(UUID uuid, String name, int entityId) {
        spyEntityIds.put(uuid, entityId);
        if (name != null) {
            spyNames.put(uuid, name.toLowerCase(Locale.ROOT));
        }
        if (entityId != PENDING) {
            entityToSpy.put(entityId, uuid);
        }
    }

    /** Updates the entity id (join, respawn, world change). */
    public void updateEntityId(UUID uuid, String name, int entityId) {
        Integer old = spyEntityIds.get(uuid);
        if (old == null) {
            return;
        }
        if (old != PENDING) {
            entityToSpy.remove(old, uuid);
        }
        addSpy(uuid, name, entityId);
    }

    public void removeSpy(UUID uuid) {
        Integer id = spyEntityIds.remove(uuid);
        if (id != null && id != PENDING) {
            entityToSpy.remove(id, uuid);
        }
        spyNames.remove(uuid);
    }

    public boolean isSpy(UUID uuid) {
        return uuid != null && spyEntityIds.containsKey(uuid);
    }

    public boolean hasSpies() {
        return !spyEntityIds.isEmpty();
    }

    public Collection<UUID> spies() {
        return Collections.unmodifiableSet(spyEntityIds.keySet());
    }

    /** Spies that are fully online (not pending login). */
    public int onlineSpyCount() {
        int count = 0;
        for (int id : spyEntityIds.values()) {
            if (id != PENDING) {
                count++;
            }
        }
        return count;
    }

    public int entityIdOf(UUID spy) {
        return spyEntityIds.getOrDefault(spy, PENDING);
    }

    public UUID spyByEntityId(int entityId) {
        return entityToSpy.get(entityId);
    }

    public UUID spyByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<UUID, String> entry : spyNames.entrySet()) {
            if (entry.getValue().equals(lower)) {
                return entry.getKey();
            }
        }
        return null;
    }

    // --------------------------------------------------------------- seers

    public void setSeer(UUID viewer, boolean seer) {
        if (seer) {
            seers.add(viewer);
        } else {
            seers.remove(viewer);
        }
    }

    public boolean isSeer(UUID viewer) {
        return seers.contains(viewer);
    }

    // ---------------------------------------------------------- visibility

    /** Whether {@code viewer} is allowed to see {@code spy}. */
    public boolean canSee(UUID viewer, UUID spy) {
        if (viewer == null || viewer.equals(spy)) {
            return true;
        }
        if (seers.contains(viewer)) {
            return true;
        }
        return config.spiesSeeEachOther && isSpy(viewer);
    }

    /** Whether {@code target} is a spy that must be hidden from {@code viewer}. */
    public boolean isHiddenFrom(UUID viewer, UUID target) {
        return isSpy(target) && !canSee(viewer, target);
    }

    /** Whether the entity id belongs to a spy that must be hidden from {@code viewer}. */
    public boolean isEntityHiddenFrom(UUID viewer, int entityId) {
        UUID spy = entityToSpy.get(entityId);
        return spy != null && !canSee(viewer, spy);
    }

    /** All spies hidden from the viewer. */
    public Set<UUID> hiddenFrom(UUID viewer) {
        Set<UUID> hidden = new HashSet<>();
        for (UUID spy : spyEntityIds.keySet()) {
            if (!canSee(viewer, spy)) {
                hidden.add(spy);
            }
        }
        return hidden;
    }

    /** Lower-case names of all spies hidden from the viewer. */
    public Set<String> hiddenNamesFrom(UUID viewer) {
        Set<String> names = new HashSet<>();
        for (Map.Entry<UUID, String> entry : spyNames.entrySet()) {
            if (!canSee(viewer, entry.getKey())) {
                names.add(entry.getValue());
            }
        }
        return names;
    }
}
