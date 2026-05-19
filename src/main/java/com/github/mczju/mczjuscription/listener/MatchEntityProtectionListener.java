package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.entity.MatchEntityProtection;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/** 对局内展示/造物实体不受伤害。 */
public final class MatchEntityProtectionListener implements Listener {

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
