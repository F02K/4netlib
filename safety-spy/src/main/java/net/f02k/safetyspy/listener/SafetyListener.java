package net.f02k.safetyspy.listener;

import net.f02k.safetyspy.SafetySpyPlugin;
import net.f02k.safetyspy.spy.SpyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockReceiveGameEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;

/**
 * Prevents interactions that could reveal a spy.
 */
public final class SafetyListener implements Listener {

    private final SafetySpyPlugin plugin;
    private final SpyManager manager;

    public SafetyListener(SafetySpyPlugin plugin, SpyManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private boolean spy(Object entity) {
        return entity instanceof Player player && manager.isSpy(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (plugin.spyConfig().invulnerable && spy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (plugin.spyConfig().noMobTarget && spy(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (plugin.spyConfig().noItemPickup && spy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowPickup(PlayerPickupArrowEvent event) {
        if (plugin.spyConfig().noItemPickup && spy(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPhysical(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL
                && plugin.spyConfig().noPhysicalInteract && spy(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSculk(BlockReceiveGameEvent event) {
        if (plugin.spyConfig().noSculk && spy(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
