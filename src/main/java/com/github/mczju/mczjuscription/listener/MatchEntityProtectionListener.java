package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.entity.MatchEntityProtection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** 对局内展示/造物实体不受伤害。 */
public final class MatchEntityProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSnowTrail(EntityBlockFormEvent event) {
        if (event.getEntity() instanceof Snowman
                && MatchEntityProtection.isProtectedMatchEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSnowTrailChange(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Snowman
                && MatchEntityProtection.isProtectedMatchEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!MatchEntityProtection.isProtectedMatchEntity(event.getEntity())) {
            return;
        }
        event.setCancelled(true);
        if (event.getEntity() instanceof LivingEntity living) {
            MatchEntityProtection.immunizeSunBurn(living);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (MatchEntityProtection.isProtectedMatchEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (MatchEntityProtection.isProtectedMatchEntity(victim)) {
            event.setCancelled(true);
            return;
        }
        if (MatchEntityProtection.isProtectedMatchEntity(event.getDamager())) {
            event.setCancelled(true);
        }
    }
}
