package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.util.InscriptionKeys;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractSkeleton;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.boss.BossBar;

/** 对局造物、商人、展示实体统一无敌与伤害拦截标记。 */
public final class MatchEntityProtection {

    private MatchEntityProtection() {}

    public static void apply(Entity entity) {
        if (entity == null) {
            return;
        }
        entity.setInvulnerable(true);
        hideBossBar(entity);
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setPersistent(true);
            immunizeSunBurn(living);
            if (living instanceof Mob mob) {
                mob.setAI(false);
                mob.setAware(false);
                mob.setCollidable(false);
                mob.setSilent(true);
            }
        }
    }

    /** 棋盘造物在露天场地不应被日光点燃（僵尸、骷髅等）。 */
    public static void immunizeSunBurn(LivingEntity living) {
        if (living instanceof Zombie zombie) {
            zombie.setShouldBurnInDay(false);
        } else if (living instanceof AbstractSkeleton skeleton) {
            skeleton.setShouldBurnInDay(false);
        } else if (living instanceof Phantom phantom) {
            phantom.setShouldBurnInDay(false);
        }
        living.setFireTicks(0);
        living.setVisualFire(TriState.FALSE);
    }

    /** 棋盘造物（如凋灵）不应显示原版 Boss 血条。 */
    public static void hideBossBar(Entity entity) {
        if (!(entity instanceof Boss boss)) {
            return;
        }
        BossBar bar = boss.getBossBar();
        if (bar == null) {
            return;
        }
        bar.setVisible(false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bar.removePlayer(player);
        }
    }

    public static boolean isProtectedMatchEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        var pdc = entity.getPersistentDataContainer();
        return pdc.has(InscriptionKeys.CREATURE_INSTANCE, PersistentDataType.STRING)
                || pdc.has(InscriptionKeys.WANDERING_TRADER, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.SHOP_VILLAGER, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.RABBIT_CHEST, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.MATCH_DISPLAY, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.ARENA_ID, PersistentDataType.STRING)
                || pdc.has(InscriptionKeys.HUB_SEAT_MARKER, PersistentDataType.BYTE);
    }
}
