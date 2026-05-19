package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.util.InscriptionKeys;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.persistence.PersistentDataType;

/** 对局造物、商人、展示实体统一无敌与伤害拦截标记。 */
public final class MatchEntityProtection {

    private MatchEntityProtection() {}

    public static void apply(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.setInvulnerable(true);
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setPersistent(true);
            if (living instanceof Mob mob) {
                mob.setAI(false);
                mob.setAware(false);
                mob.setCollidable(false);
                mob.setSilent(true);
            }
        }
    }

    public static boolean isProtectedMatchEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(InscriptionKeys.CREATURE_INSTANCE, PersistentDataType.STRING)
                || pdc.has(InscriptionKeys.WANDERING_TRADER, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.MATCH_DISPLAY, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.ARENA_ID, PersistentDataType.STRING)
                || pdc.has(InscriptionKeys.HUB_SEAT_MARKER, PersistentDataType.BYTE);
    }
}
