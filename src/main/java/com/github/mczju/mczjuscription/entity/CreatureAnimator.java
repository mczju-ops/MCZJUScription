package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** 造物实体移动与攻击表现（配合战斗顺序播放）。 */
public final class CreatureAnimator {

    public static final int MOVE_TICKS = 10;
    public static final int PAUSE_TICKS = 8;

    private CreatureAnimator() {}

    public static Location slotStand(Location slotCenter) {
        if (slotCenter == null || slotCenter.getWorld() == null) return null;
        return slotCenter.clone();
    }

    /**
     * 冲向目标 → 回调（结算）→ 退回原位。
     */
    public static void playAttackSequence(
            BoardCreature attacker,
            Location home,
            Location target,
            Runnable onStrike,
            Runnable onComplete
    ) {
        if (home == null || target == null) {
            if (onStrike != null) onStrike.run();
            if (onComplete != null) onComplete.run();
            return;
        }
        Location lungeTarget = lungePoint(home, target);
        animateMove(attacker, lungeTarget, MOVE_TICKS, () -> {
            playSwing(attacker);
            if (onStrike != null) onStrike.run();
            animateMove(attacker, home, MOVE_TICKS, () -> {
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /** 沿槽位路径平移（预览区前进等）。 */
    public static void playMoveSequence(
            BoardCreature creature,
            Location from,
            Location to,
            Location faceToward,
            Runnable onComplete
    ) {
        if (from == null || to == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        BoardVfx.playMove(from, to);
        Location stand = slotStand(to);
        Float faceYaw = faceToward != null && stand != null
                ? ArenaFacing.yawFacing(stand, faceToward)
                : null;
        animateMove(creature, stand, MOVE_TICKS + 4, faceYaw, onComplete);
    }

    public static void animateMove(BoardCreature creature, Location end, int ticks, Runnable onComplete) {
        animateMove(creature, end, ticks, null, onComplete);
    }

    public static void animateMove(BoardCreature creature, Location end, int ticks, Float faceYaw, Runnable onComplete) {
        LivingEntity mob = living(creature);
        if (mob == null || end == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Entity display = display(creature);
        Location start = mob.getLocation().clone();
        Location goal = end.clone();
        if (faceYaw != null) {
            goal.setYaw(faceYaw);
            goal.setPitch(0f);
        } else {
            goal.setYaw(start.getYaw());
            goal.setPitch(start.getPitch());
        }

        double labelOffset = mob.getHeight() + 0.35;

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!mob.isValid()) {
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                if (step >= ticks) {
                    mob.teleport(goal);
                    if (display != null && display.isValid()) {
                        display.teleport(goal.clone().add(0, labelOffset, 0));
                    }
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                double t = (step + 1.0) / ticks;
                Location at = lerp(start, goal, t);
                mob.teleport(at);
                if (display != null && display.isValid()) {
                    display.teleport(at.clone().add(0, labelOffset, 0));
                }
                if (step % 2 == 0) {
                    at.getWorld().spawnParticle(Particle.CLOUD, at.clone().add(0, 0.5, 0), 2, 0.05, 0.05, 0.05, 0.01);
                }
                step++;
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
    }

    public static void playSwing(BoardCreature creature) {
        LivingEntity mob = living(creature);
        if (mob == null) return;
        mob.swingMainHand();
        mob.getWorld().playSound(mob.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.1f);
    }

    public static void schedule(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(MCZJUScriptionPlugin.getInstance(), task, delayTicks);
    }

    private static Location lungePoint(Location home, Location target) {
        Vector delta = target.toVector().subtract(home.toVector());
        double len = delta.length();
        if (len < 0.01) return target.clone();
        return home.clone().add(delta.multiply(Math.min(1.0, 0.65 / len)));
    }

    private static Location lerp(Location a, Location b, double t) {
        return new Location(
                a.getWorld(),
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t,
                a.getYaw(),
                a.getPitch()
        );
    }

    private static LivingEntity living(BoardCreature creature) {
        if (creature == null || creature.entityId() == null) return null;
        Entity entity = Bukkit.getEntity(creature.entityId());
        return entity instanceof LivingEntity living ? living : null;
    }

    private static Entity display(BoardCreature creature) {
        if (creature == null || creature.displayEntityId() == null) return null;
        return Bukkit.getEntity(creature.displayEntityId());
    }
}
