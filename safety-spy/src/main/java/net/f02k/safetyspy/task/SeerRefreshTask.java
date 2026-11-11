package net.f02k.safetyspy.task;

import net.f02k.safetyspy.spy.SpyManager;

/**
 * Picks up permission changes of viewers (e.g. a player gets the see permission).
 */
public final class SeerRefreshTask implements Runnable {

    private final SpyManager manager;

    public SeerRefreshTask(SpyManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.refreshSeers();
    }
}
