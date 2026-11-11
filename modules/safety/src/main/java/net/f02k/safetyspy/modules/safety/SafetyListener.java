package net.f02k.safetyspy.modules.safety;

import net.f02k.safetyspy.api.ModuleContext;
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

    private final ModuleContext context;

    public SafetyListener(ModuleContext context) {
        this.context = context;
    }

    private boolean spy(Object entity) {
        return entity instanceof Player player && context.spyManager().isSpy(player);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (context.config().invulnerable && spy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (context.config().noMobTarget && spy(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (context.config().noItemPickup && spy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowPickup(PlayerPickupArrowEvent event) {
        if (context.config().noItemPickup && spy(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPhysical(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL
                && context.config().noPhysicalInteract && spy(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSculk(BlockReceiveGameEvent event) {
        if (context.config().noSculk && spy(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
