package com.github.mczju.mczjuscription.vfx;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;

/** 【墨水】：受影响造物头顶持续渲染烟雾，效果结束即停。 */
public final class InkAuraVfx {

  private static final long INTERVAL_TICKS = 5L;
  private static final Map<UUID, BukkitTask> TASKS = new ConcurrentHashMap<>();

  private InkAuraVfx() {}

  public static void start(BoardCreature creature) {
    if (creature == null || !creature.willSkipNextAttack()) {
      return;
    }
    MCZJUScriptionPlugin plugin = MCZJUScriptionPlugin.getInstance();
    if (plugin == null) {
      return;
    }

    UUID key = creature.instanceId();
    stop(creature);

    BukkitTask task =
        Bukkit.getScheduler()
            .runTaskTimer(
                plugin,
                () -> tick(creature),
                0L,
                INTERVAL_TICKS);
    TASKS.put(key, task);
  }

  public static void stop(BoardCreature creature) {
    if (creature == null) {
      return;
    }
    BukkitTask task = TASKS.remove(creature.instanceId());
    if (task != null) {
      task.cancel();
    }
  }

  public static void stopAll() {
    for (BukkitTask task : TASKS.values()) {
      task.cancel();
    }
    TASKS.clear();
  }

  private static void tick(BoardCreature creature) {
    if (!creature.willSkipNextAttack()) {
      stop(creature);
      return;
    }
    UUID bodyId = creature.entityId();
    if (bodyId == null) {
      return;
    }
    Entity body = Bukkit.getEntity(bodyId);
    if (body == null || !body.isValid()) {
      stop(creature);
      return;
    }

    Location at = headAnchor(body);
    if (at == null || at.getWorld() == null) {
      return;
    }
    at.getWorld()
        .spawnParticle(
            Particle.LARGE_SMOKE,
            at,
            5,
            0.18,
            0.12,
            0.18,
            0.008);
    at.getWorld()
        .spawnParticle(
            Particle.SMOKE,
            at.clone().add(0, 0.15, 0),
            3,
            0.12,
            0.08,
            0.12,
            0.01);
  }

  private static Location headAnchor(Entity body) {
    Location base = body.getLocation();
    double yOffset =
        body instanceof LivingEntity living
            ? living.getHeight() + 0.45
            : (body instanceof BlockDisplay ? 0.75 : 0.55);
    return base.clone().add(0, yOffset, 0);
  }
}
