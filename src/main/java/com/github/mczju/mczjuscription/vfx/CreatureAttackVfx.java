package com.github.mczju.mczjuscription.vfx;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** 按造物实体类型播放攻击音效与特效。 */
public final class CreatureAttackVfx {

  private static final int ARROW_TRAVEL_TICKS = 4;

  private CreatureAttackVfx() {}

  public static void play(
      BoardCreature attacker, Location from, Location to, Runnable onImpact) {
    if (attacker == null || from == null || to == null || from.getWorld() == null) {
      if (onImpact != null) {
        onImpact.run();
      }
      return;
    }
    if (!from.getWorld().equals(to.getWorld())) {
      if (onImpact != null) {
        onImpact.run();
      }
      return;
    }

    Location launch = aimPoint(from);
    Location impact = aimPoint(to);
    EntityType type = attacker.template().entityType();
    playAttackSound(attacker, type, launch);

    if (isSkeletonSeries(type)) {
      playArrowStrike(launch, impact, onImpact);
      return;
    }
    if (type == EntityType.WARDEN) {
      playWardenSonic(launch, impact, onImpact);
      return;
    }
    if (type == EntityType.GUARDIAN) {
      playGuardianBeam(launch, impact, false, onImpact);
      return;
    }
    if (type == EntityType.ELDER_GUARDIAN) {
      playGuardianBeam(launch, impact, true, onImpact);
      return;
    }
    if (onImpact != null) {
      onImpact.run();
    }
  }

  private static void playAttackSound(BoardCreature creature, EntityType type, Location at) {
    World world = at.getWorld();
    Sound mapped = mappedAttackSound(type);
    if (mapped != null) {
      world.playSound(at, mapped, 0.95f, 1.0f);
      return;
    }
    if (creature.entityId() != null) {
      Entity body = Bukkit.getEntity(creature.entityId());
      if (body instanceof LivingEntity living) {
        Sound hurt = living.getHurtSound();
        if (hurt != null) {
          world.playSound(at, hurt, 0.85f, 1.15f);
          return;
        }
      }
    }
    world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.75f, 1.05f);
  }

  private static Sound mappedAttackSound(EntityType type) {
    return switch (type) {
      case WARDEN -> Sound.ENTITY_WARDEN_SONIC_BOOM;
      case GUARDIAN -> Sound.ENTITY_GUARDIAN_ATTACK;
      case ELDER_GUARDIAN -> Sound.ENTITY_ELDER_GUARDIAN_CURSE;
      case SKELETON, STRAY, BOGGED -> Sound.ENTITY_SKELETON_SHOOT;
      case WITHER_SKELETON -> Sound.ENTITY_PLAYER_ATTACK_SWEEP;
      case BLAZE -> Sound.ENTITY_BLAZE_SHOOT;
      case GHAST -> Sound.ENTITY_GHAST_SHOOT;
      case EVOKER -> Sound.ENTITY_EVOKER_CAST_SPELL;
      case VINDICATOR -> Sound.ENTITY_VINDICATOR_AMBIENT;
      case RAVAGER -> Sound.ENTITY_RAVAGER_ROAR;
      case WOLF -> Sound.ENTITY_WOLF_GROWL;
      case CREEPER -> Sound.ENTITY_CREEPER_HURT;
      case ENDERMAN -> Sound.ENTITY_ENDERMAN_SCREAM;
      case SPIDER, CAVE_SPIDER -> Sound.ENTITY_SPIDER_AMBIENT;
      case ZOMBIE, ZOMBIE_VILLAGER, HUSK, DROWNED -> Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR;
      case PIGLIN, PIGLIN_BRUTE -> Sound.ENTITY_PIGLIN_ANGRY;
      case HOGLIN -> Sound.ENTITY_HOGLIN_ANGRY;
      case RABBIT -> Sound.ENTITY_RABBIT_ATTACK;
      case LLAMA -> Sound.ENTITY_LLAMA_SPIT;
      case SHULKER -> Sound.ENTITY_SHULKER_SHOOT;
      case VEX -> Sound.ENTITY_VEX_CHARGE;
      case WITCH -> Sound.ENTITY_WITCH_THROW;
      case IRON_GOLEM -> Sound.ENTITY_IRON_GOLEM_ATTACK;
      case SNOW_GOLEM -> Sound.ENTITY_SNOW_GOLEM_SHOOT;
      case BEE -> Sound.ENTITY_BEE_STING;
      case DOLPHIN -> Sound.ENTITY_DOLPHIN_ATTACK;
      case POLAR_BEAR -> Sound.ENTITY_POLAR_BEAR_WARNING;
      case PANDA -> Sound.ENTITY_PANDA_AGGRESSIVE_AMBIENT;
      case GOAT -> Sound.ENTITY_GOAT_SCREAMING_AMBIENT;
      case FROG -> Sound.ENTITY_FROG_LONG_JUMP;
      case ALLAY -> Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM;
      default -> null;
    };
  }

  private static boolean isSkeletonSeries(EntityType type) {
    return type == EntityType.SKELETON
        || type == EntityType.STRAY
        || type == EntityType.BOGGED;
  }

  private static void playArrowStrike(Location from, Location to, Runnable onImpact) {
    World world = from.getWorld();
    Vector delta = to.toVector().subtract(from.toVector());
    double distance = delta.length();
    if (distance < 0.05) {
      if (onImpact != null) {
        onImpact.run();
      }
      return;
    }

    Vector direction = delta.clone().normalize();
    float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));
    float pitch = (float) Math.toDegrees(-Math.asin(clamp(direction.getY(), -1, 1)));

    Arrow arrow =
        world.spawn(
            from.clone(),
            Arrow.class,
            spawned -> {
              spawned.setGravity(false);
              spawned.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
              spawned.setDamage(0);
              spawned.setCritical(false);
              spawned.setKnockbackStrength(0);
              spawned.setSilent(true);
              spawned.setInvulnerable(true);
              spawned.setVelocity(new Vector(0, 0, 0));
              spawned.setRotation(yaw, pitch);
            });

    int ticks = Math.max(3, Math.min(ARROW_TRAVEL_TICKS, (int) Math.ceil(distance * 1.5)));
    new BukkitRunnable() {
      int step = 0;

      @Override
      public void run() {
        if (!arrow.isValid()) {
          if (onImpact != null) {
            onImpact.run();
          }
          cancel();
          return;
        }
        if (step >= ticks) {
          arrow.remove();
          if (onImpact != null) {
            onImpact.run();
          }
          cancel();
          return;
        }
        double t = (step + 1.0) / ticks;
        Location at = lerp(from, to, t);
        at.setYaw(yaw);
        at.setPitch(pitch);
        arrow.teleport(at);
        step++;
      }
    }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
  }

  private static void playWardenSonic(Location from, Location to, Runnable onImpact) {
    traceBeam(from, to, Particle.SONIC_BOOM, 1, 0, 0, 0, 0);
    traceBeam(from, to, Particle.END_ROD, 2, 0.03, 0.03, 0.03, 0.01);
    Location hit = aimPoint(to);
    World world = hit.getWorld();
    world.spawnParticle(Particle.SONIC_BOOM, hit, 2, 0.08, 0.12, 0.08, 0);
    world.spawnParticle(Particle.EXPLOSION, hit, 1, 0, 0, 0, 0);
    if (onImpact != null) {
      onImpact.run();
    }
  }

  private static void playGuardianBeam(
      Location from, Location to, boolean elder, Runnable onImpact) {
    Color beamColor = elder ? Color.fromRGB(70, 180, 210) : Color.fromRGB(90, 220, 255);
    traceColoredBeam(from, to, beamColor, elder ? 3 : 2);
    traceBeam(from, to, Particle.END_ROD, elder ? 3 : 2, 0.02, 0.02, 0.02, 0.01);
    Location hit = aimPoint(to);
    hit.getWorld().spawnParticle(Particle.WITCH, hit, elder ? 10 : 6, 0.12, 0.18, 0.12, 0.02);
    if (onImpact != null) {
      onImpact.run();
    }
  }

  private static void traceBeam(
      Location from,
      Location to,
      Particle particle,
      int count,
      double offsetX,
      double offsetY,
      double offsetZ,
      double extra) {
    World world = from.getWorld();
    int steps = beamSteps(from, to);
    for (int i = 0; i <= steps; i++) {
      Location at = lerp(from, to, i / (double) steps);
      world.spawnParticle(particle, at, count, offsetX, offsetY, offsetZ, extra);
    }
  }

  private static void traceColoredBeam(Location from, Location to, Color color, int count) {
    World world = from.getWorld();
    Particle.DustOptions dust = new Particle.DustOptions(color, elderSize(from, to));
    int steps = beamSteps(from, to);
    for (int i = 0; i <= steps; i++) {
      Location at = lerp(from, to, i / (double) steps);
      world.spawnParticle(Particle.DUST, at, count, 0.02, 0.02, 0.02, 0, dust);
    }
  }

  private static float elderSize(Location from, Location to) {
    return Math.min(1.4f, 0.8f + (float) from.distance(to) * 0.08f);
  }

  private static int beamSteps(Location from, Location to) {
    return Math.max(10, (int) Math.ceil(from.distance(to) * 5));
  }

  private static Location aimPoint(Location base) {
    return base.clone().add(0, 0.95, 0);
  }

  private static Location lerp(Location a, Location b, double t) {
    return new Location(
        a.getWorld(),
        a.getX() + (b.getX() - a.getX()) * t,
        a.getY() + (b.getY() - a.getY()) * t,
        a.getZ() + (b.getZ() - a.getZ()) * t);
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
