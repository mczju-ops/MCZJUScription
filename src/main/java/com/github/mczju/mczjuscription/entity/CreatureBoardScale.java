package com.github.mczju.mczjuscription.entity;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

/** 棋盘造物实体体积修正（部分原版模型过大，不适合槽位展示）。 */
public final class CreatureBoardScale {

  /** 与 /attribute @e minecraft:generic.scale base set 等价。 */
  private static final Map<EntityType, Double> BOARD_SCALES =
      Map.of(
          EntityType.GHAST, 0.5);

  private CreatureBoardScale() {}

  public static double scaleFor(EntityType type) {
    if (type == null) {
      return 1.0;
    }
    return BOARD_SCALES.getOrDefault(type, 1.0);
  }

  public static void apply(LivingEntity entity, EntityType type) {
    if (entity == null || type == null) {
      return;
    }
    double scale = scaleFor(type);
    if (Math.abs(scale - 1.0) < 1e-6) {
      return;
    }
    if (scale <= 0.01 || scale > 10.0) {
      return;
    }
    AttributeInstance attr = entity.getAttribute(Attribute.SCALE);
    if (attr != null) {
      attr.setBaseValue(scale);
    }
  }
}
