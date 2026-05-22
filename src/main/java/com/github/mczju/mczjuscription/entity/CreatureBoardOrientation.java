package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fish;
import org.bukkit.entity.LivingEntity;

/** 棋盘造物实体朝向修正（如鱼类在陆地上默认平躺）。 */
public final class CreatureBoardOrientation {

  /** 鱼类在陆地上模型横躺；棋盘展示改为竖立，仍用 yaw 朝向对手。 */
  private static final float FISH_BOARD_PITCH = -90f;

  /** 【空袭】造物悬浮于槽位上方。 */
  public static final double AIR_STRIKE_Y_OFFSET = 1.0;

  private CreatureBoardOrientation() {}

  public static double boardYOffset(BoardCreature creature) {
    if (creature != null && creature.hasSigil(SigilId.AIR_STRIKE)) {
      return AIR_STRIKE_Y_OFFSET;
    }
    return 0.0;
  }

  public static Location applyBoardStand(BoardCreature creature, Location slotCenter) {
    if (slotCenter == null || slotCenter.getWorld() == null) {
      return null;
    }
    Location at = slotCenter.clone();
    at.add(0, boardYOffset(creature), 0);
    return at;
  }

  public static boolean isFishEntity(LivingEntity entity) {
    return entity instanceof Fish;
  }

  public static boolean isFishType(EntityType type) {
    if (type == null || !type.isAlive()) {
      return false;
    }
    Class<? extends Entity> clazz = type.getEntityClass();
    return clazz != null && Fish.class.isAssignableFrom(clazz);
  }

  public static float boardPitch(LivingEntity entity) {
    return isFishEntity(entity) ? FISH_BOARD_PITCH : 0f;
  }

  public static void applySpawnPose(LivingEntity entity, BoardCreature creature, Location spawnAt) {
    if (entity == null || spawnAt == null) {
      return;
    }
    if (isFishEntity(entity) || (creature != null && creature.hasSigil(SigilId.AIR_STRIKE))) {
      entity.setGravity(false);
    }
    float yaw = spawnAt.getYaw();
    float pitch = boardPitch(entity);
    entity.setRotation(yaw, pitch);
    Location at = entity.getLocation();
    at.setYaw(yaw);
    at.setPitch(pitch);
    entity.teleport(at);
  }
}
