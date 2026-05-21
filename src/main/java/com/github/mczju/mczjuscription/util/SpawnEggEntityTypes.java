package com.github.mczju.mczjuscription.util;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

public final class SpawnEggEntityTypes {

  private SpawnEggEntityTypes() {}

  public static Optional<EntityType> fromSpawnEgg(ItemStack stack) {
    if (stack == null || stack.getType().isAir()) {
      return Optional.empty();
    }
    Material material = stack.getType();
    String name = material.name();
    if (!name.endsWith("_SPAWN_EGG")) {
      return Optional.empty();
    }
    String entityKey = name.substring(0, name.length() - "_SPAWN_EGG".length());
    try {
      return Optional.of(EntityType.valueOf(entityKey));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public static String displayEntity(EntityType type) {
    if (type == null) {
      return "?";
    }
    String raw = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
  }
}
