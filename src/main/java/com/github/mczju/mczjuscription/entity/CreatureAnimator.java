package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** 造物实体移动与攻击表现（配合战斗顺序播放）。 */
public final class CreatureAnimator {

    public static final int MOVE_TICKS = 10;
    public static final int PAUSE_TICKS = 8;
    public static final int PUSH_TICKS = 9;
    public static final int BREEZE_CHARGE_TICKS = 5;
    public static final int BREEZE_JUMP_TICKS = 10;

    private CreatureAnimator() {}

    public static Location slotStand(BoardCreature creature, Location slotCenter) {
        return CreatureBoardOrientation.applyBoardStand(creature, slotCenter);
    }

    /**
     * 冲向目标 → 回调（结算）→ 退回原位；退回后恢复 {@code faceToward} 朝向（通常为朝对手）。
     */
    public static void playAttackSequence(
            BoardCreature attacker,
            Location home,
            Location target,
            Location faceToward,
            Runnable onStrike,
            Runnable onComplete
    ) {
        if (home == null || target == null) {
            if (onStrike != null) onStrike.run();
            if (onComplete != null) onComplete.run();
            return;
        }
        Location lungeTarget = lungePoint(home, target);
        Float settleFaceYaw =
                faceToward != null ? ArenaFacing.yawFacing(home, faceToward) : null;
        Location homeStand = home.clone();
        if (settleFaceYaw != null) {
            homeStand.setYaw(settleFaceYaw);
            LivingEntity mob = living(attacker);
            if (mob != null) {
                homeStand.setPitch(CreatureBoardOrientation.boardPitch(mob));
            }
        }
        animateMove(attacker, lungeTarget, MOVE_TICKS, () -> {
            playSwing(attacker);
            if (onStrike != null) onStrike.run();
            animateMove(attacker, homeStand, MOVE_TICKS, settleFaceYaw, () -> {
                snapToStand(attacker, homeStand);
                if (onComplete != null) onComplete.run();
            });
        });
    }

    /** 沿槽位路径平移；移动中朝向行进方向，落位后 {@code faceToward} 朝对手。 */
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
        BoardVfx.playMove(from, to, MOVE_TICKS + 4);
        Location start = slotStand(creature, from);
        Location end = slotStand(creature, to);
        Float settleFaceYaw = faceToward != null && end != null
                ? ArenaFacing.yawFacing(end, faceToward)
                : null;
        if (settleFaceYaw != null && end != null) {
            end.setYaw(settleFaceYaw);
            LivingEntity mob = living(creature);
            if (mob != null) {
                end.setPitch(CreatureBoardOrientation.boardPitch(mob));
            }
        }
        Location endStand = end;
        animateMoveFromTo(creature, start, end, MOVE_TICKS + 4, settleFaceYaw, () -> {
            snapToStand(creature, endStand);
            if (onComplete != null) onComplete.run();
        });
    }

    /** 【蛮力】推挤：加速滑出 + 落地冲击，再回调推挤者移动。 */
    public static void playPushSlide(
            BoardCreature pushed,
            Location from,
            Location to,
            Runnable onComplete) {
        if (from == null || to == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        World world = from.getWorld();
        if (world != null) {
            world.playSound(from, Sound.ENTITY_RAVAGER_ATTACK, 0.45f, 1.5f);
        }
        BoardVfx.playPushTrail(from, to, PUSH_TICKS + 2);
        Location start = slotStand(pushed, from);
        Location end = slotStand(pushed, to);
        animateMoveFromTo(
                pushed,
                start,
                end,
                PUSH_TICKS,
                null,
                () -> {
                    BoardVfx.playPushImpact(to);
                    if (onComplete != null) onComplete.run();
                });
    }

    /** 将造物实体与标签对齐到 {@code stand} 的位置与朝向。 */
    public static void snapToStand(BoardCreature creature, Location stand) {
        if (stand == null) return;
        LivingEntity mob = living(creature);
        if (mob != null) {
            float boardPitch = CreatureBoardOrientation.boardPitch(mob);
            Location goal = stand.clone();
            goal.setPitch(boardPitch);
            mob.teleport(goal);
            Entity label = display(creature);
            if (label != null && label.isValid()) {
                label.teleport(goal.clone().add(0, mob.getHeight() + 0.35, 0));
            }
            return;
        }
        Entity body = body(creature);
        if (body == null || !body.isValid()) return;
        Location goal = stand.clone();
        body.teleport(goal);
        Entity label = display(creature);
        if (label != null && label.isValid()) {
            double labelOffset = body instanceof BlockDisplay ? 0.55 : 0.35;
            label.teleport(goal.clone().add(0, labelOffset, 0));
        }
    }

    public static void animateMoveFromTo(
            BoardCreature creature,
            Location start,
            Location end,
            int ticks,
            Float settleFaceYaw,
            Runnable onComplete) {
        if (end == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        if (start != null) {
            Location stand = start.clone();
            applyTravelFacing(stand, start, end, creature);
            snapToStand(creature, stand);
        }
        LivingEntity mob = living(creature);
        if (mob != null) {
            animateLivingMove(creature, mob, start, end, ticks, settleFaceYaw, onComplete);
            return;
        }
        Entity body = body(creature);
        if (body != null) {
            animateDisplayMove(creature, body, start, end, ticks, settleFaceYaw, onComplete);
            return;
        }
        if (onComplete != null) onComplete.run();
    }

    /**
     * 旋风人式跳跃：蓄力压缩 → 弧线跃起 → 落地（对应 entity.breeze.charge/jump/land）。
     */
    public static void playBreezeJump(
            BoardCreature creature, Location fromSlot, Location toSlot, Runnable onComplete) {
        if (fromSlot == null || toSlot == null || fromSlot.getWorld() == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        Location start = resolveMoveStart(creature, fromSlot);
        Location end = slotStand(creature, toSlot);
        if (start == null || end == null || !start.getWorld().equals(end.getWorld())) {
            if (onComplete != null) onComplete.run();
            return;
        }

        World world = start.getWorld();
        world.playSound(start, Sound.ENTITY_BREEZE_CHARGE, 1.0f, 1.0f);
        spawnBreezeChargeFx(world, start);

        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> {
                            world.playSound(start, Sound.ENTITY_BREEZE_JUMP, 1.0f, 1.0f);
                            animateArcMove(creature, start, end, BREEZE_JUMP_TICKS, () -> {
                                world.playSound(end, Sound.ENTITY_BREEZE_LAND, 1.0f, 1.0f);
                                world.spawnParticle(
                                        Particle.GUST,
                                        end.clone().add(0, 0.45, 0),
                                        14,
                                        0.28,
                                        0.18,
                                        0.28,
                                        0.02);
                                world.spawnParticle(
                                        Particle.POOF, end, 8, 0.15, 0.12, 0.15, 0.02);
                                if (onComplete != null) onComplete.run();
                            });
                        },
                        BREEZE_CHARGE_TICKS);
    }

    public static void animateMove(BoardCreature creature, Location end, int ticks, Runnable onComplete) {
        animateMove(creature, end, ticks, null, onComplete);
    }

    public static void animateMove(BoardCreature creature, Location end, int ticks, Float settleFaceYaw, Runnable onComplete) {
        if (end == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        LivingEntity mob = living(creature);
        if (mob != null) {
            animateLivingMove(creature, mob, null, end, ticks, settleFaceYaw, onComplete);
            return;
        }
        Entity body = body(creature);
        if (body != null) {
            animateDisplayMove(creature, body, null, end, ticks, settleFaceYaw, onComplete);
            return;
        }
        if (onComplete != null) onComplete.run();
    }

    private static void animateLivingMove(
            BoardCreature creature,
            LivingEntity mob,
            Location start,
            Location end,
            int ticks,
            Float settleFaceYaw,
            Runnable onComplete) {
        Entity label = display(creature);
        Location goalPos = end.clone();
        float boardPitch = CreatureBoardOrientation.boardPitch(mob);
        Location from = start != null ? start.clone() : mob.getLocation().clone();
        Float travelYaw = movementYaw(from, goalPos);
        float moveYaw = travelYaw != null ? travelYaw : from.getYaw();
        float finalYaw = settleFaceYaw != null ? settleFaceYaw : moveYaw;

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
                    Location goal = goalPos.clone();
                    goal.setYaw(finalYaw);
                    goal.setPitch(boardPitch);
                    mob.teleport(goal);
                    if (label != null && label.isValid()) {
                        label.teleport(goal.clone().add(0, labelOffset, 0));
                    }
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                double t = (step + 1.0) / ticks;
                Location at = lerpPosition(from, goalPos, t);
                at.setYaw(moveYaw);
                at.setPitch(boardPitch);
                mob.teleport(at);
                if (label != null && label.isValid()) {
                    label.teleport(at.clone().add(0, labelOffset, 0));
                }
                step++;
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
    }

    private static void animateDisplayMove(
            BoardCreature creature,
            Entity body,
            Location start,
            Location end,
            int ticks,
            Runnable onComplete) {
        animateDisplayMove(creature, body, start, end, ticks, null, onComplete);
    }

    private static void animateDisplayMove(
            BoardCreature creature,
            Entity body,
            Location start,
            Location end,
            int ticks,
            Float settleFaceYaw,
            Runnable onComplete) {
        Entity label = display(creature);
        Location from = start != null ? start.clone() : body.getLocation().clone();
        Location goalPos = end.clone();
        Float travelYaw = movementYaw(from, goalPos);
        float moveYaw = travelYaw != null ? travelYaw : from.getYaw();
        float finalYaw = settleFaceYaw != null ? settleFaceYaw : moveYaw;
        float pitch = from.getPitch();
        double labelOffset = body instanceof BlockDisplay ? 0.55 : 0.35;

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!body.isValid()) {
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                if (step >= ticks) {
                    Location goal = goalPos.clone();
                    goal.setYaw(finalYaw);
                    goal.setPitch(pitch);
                    body.teleport(goal);
                    if (label != null && label.isValid()) {
                        label.teleport(goal.clone().add(0, labelOffset, 0));
                    }
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                double t = (step + 1.0) / ticks;
                Location at = lerpPosition(from, goalPos, t);
                at.setYaw(moveYaw);
                at.setPitch(pitch);
                body.teleport(at);
                if (label != null && label.isValid()) {
                    label.teleport(at.clone().add(0, labelOffset, 0));
                }
                step++;
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
    }

    private static void animateArcMove(
            BoardCreature creature, Location start, Location end, int ticks, Runnable onComplete) {
        LivingEntity mob = living(creature);
        if (mob != null) {
            animateLivingArc(creature, mob, start, end, ticks, onComplete);
            return;
        }
        Entity body = body(creature);
        if (body != null) {
            animateDisplayArc(creature, body, start, end, ticks, onComplete);
            return;
        }
        if (onComplete != null) onComplete.run();
    }

    private static void animateLivingArc(
            BoardCreature creature,
            LivingEntity mob,
            Location start,
            Location end,
            int ticks,
            Runnable onComplete) {
        Entity label = display(creature);
        Location goal = end.clone();
        float boardPitch = CreatureBoardOrientation.boardPitch(mob);
        goal.setYaw(ArenaFacing.yawFacing(start, end));
        goal.setPitch(boardPitch);
        double labelOffset = mob.getHeight() + 0.35;
        double arcHeight = arcHeight(start, end);

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
                    if (label != null && label.isValid()) {
                        label.teleport(goal.clone().add(0, labelOffset, 0));
                    }
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                double t = (step + 1.0) / ticks;
                Location at = arcPoint(start, goal, t, arcHeight);
                at.setYaw(ArenaFacing.yawFacing(at, goal));
                at.setPitch(boardPitch);
                mob.teleport(at);
                if (label != null && label.isValid()) {
                    label.teleport(at.clone().add(0, labelOffset, 0));
                }
                if (step % 2 == 0) {
                    at.getWorld()
                            .spawnParticle(
                                    Particle.GUST,
                                    at.clone().add(0, 0.35, 0),
                                    2,
                                    0.06,
                                    0.06,
                                    0.06,
                                    0.01);
                }
                step++;
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
    }

    private static void animateDisplayArc(
            BoardCreature creature,
            Entity body,
            Location start,
            Location end,
            int ticks,
            Runnable onComplete) {
        Entity label = display(creature);
        Location goal = end.clone();
        goal.setYaw(body.getLocation().getYaw());
        goal.setPitch(body.getLocation().getPitch());
        double labelOffset = body instanceof BlockDisplay ? 0.55 : 0.35;
        double arcHeight = arcHeight(start, end);

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!body.isValid()) {
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                if (step >= ticks) {
                    body.teleport(goal);
                    if (label != null && label.isValid()) {
                        label.teleport(goal.clone().add(0, labelOffset, 0));
                    }
                    cancel();
                    if (onComplete != null) onComplete.run();
                    return;
                }
                double t = (step + 1.0) / ticks;
                Location at = arcPoint(start, goal, t, arcHeight);
                body.teleport(at);
                if (label != null && label.isValid()) {
                    label.teleport(at.clone().add(0, labelOffset, 0));
                }
                step++;
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
    }

    public static void playSwing(BoardCreature creature) {
        LivingEntity mob = living(creature);
        if (mob == null) return;
        mob.swingMainHand();
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

    private static Location resolveMoveStart(BoardCreature creature, Location fromSlot) {
        Entity body = body(creature);
        if (body != null && body.isValid()) {
            return body.getLocation().clone();
        }
        return slotStand(creature, fromSlot);
    }

    private static double arcHeight(Location start, Location end) {
        return Math.min(1.8, 0.35 + start.distance(end) * 0.25);
    }

    private static Location arcPoint(Location start, Location end, double t, double height) {
        Location at = lerp(start, end, t);
        at.setY(at.getY() + height * 4.0 * t * (1.0 - t));
        return at;
    }

    private static void spawnBreezeChargeFx(World world, Location at) {
        world.spawnParticle(Particle.GUST, at.clone().add(0, 0.25, 0), 6, 0.12, 0.08, 0.12, 0.01);
        world.spawnParticle(Particle.CLOUD, at.clone().add(0, 0.35, 0), 4, 0.08, 0.06, 0.08, 0.01);
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

    private static Location lerpPosition(Location a, Location b, double t) {
        return new Location(
                a.getWorld(),
                a.getX() + (b.getX() - a.getX()) * t,
                a.getY() + (b.getY() - a.getY()) * t,
                a.getZ() + (b.getZ() - a.getZ()) * t,
                0f,
                0f);
    }

    /** 水平位移足够大时，返回沿路径的朝向；否则保持原朝向。 */
    private static Float movementYaw(Location from, Location to) {
        if (from == null || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return null;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        if (dx * dx + dz * dz < 0.0025) {
            return null;
        }
        return ArenaFacing.yawFacing(from, to);
    }

    private static void applyTravelFacing(
            Location stand, Location from, Location to, BoardCreature creature) {
        Float yaw = movementYaw(from, to);
        if (yaw == null) {
            return;
        }
        stand.setYaw(yaw);
        LivingEntity mob = living(creature);
        if (mob != null) {
            stand.setPitch(CreatureBoardOrientation.boardPitch(mob));
        }
    }

    private static LivingEntity living(BoardCreature creature) {
        if (creature == null || creature.entityId() == null) return null;
        Entity entity = Bukkit.getEntity(creature.entityId());
        if (entity instanceof BlockDisplay) {
            return null;
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    private static Entity body(BoardCreature creature) {
        if (creature == null || creature.entityId() == null) return null;
        return Bukkit.getEntity(creature.entityId());
    }

    private static Entity display(BoardCreature creature) {
        if (creature == null || creature.displayEntityId() == null) return null;
        return Bukkit.getEntity(creature.displayEntityId());
    }
}
