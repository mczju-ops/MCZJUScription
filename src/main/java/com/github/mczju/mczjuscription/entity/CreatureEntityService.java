package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import com.github.mczjuops.mczjugamecore.utils.TextParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public final class CreatureEntityService {

  private static final double LABEL_HEIGHT = 0.35;

  private CreatureEntityService() {}

  public static void spawn(BoardCreature creature, Location at) {
    Location spawnAt = at.clone();
    if (spawnAt.getWorld() == null) {
      return;
    }

    SpawnBundle bundle = trySpawnLiving(creature, spawnAt);
    if (bundle == null) {
      bundle = spawnFallback(creature, spawnAt);
    }
    if (bundle == null) {
      return;
    }
    creature.bindEntity(bundle.bodyId(), bundle.labelId());
  }

  public static void refreshLabel(BoardCreature creature) {
    UUID displayId = creature.displayEntityId();
    if (displayId == null) return;
    Entity entity = findEntity(displayId);
    if (entity instanceof TextDisplay display) {
      display.text(buildLabel(creature));
    }
  }

  /** 战斗选目标等高亮（射线确认前）。 */
  public static void setCombatGlow(BoardCreature creature, boolean enabled) {
    Entity entity = findEntity(creature.entityId());
    if (!(entity instanceof LivingEntity living)) {
      return;
    }
    if (enabled) {
      living.addPotionEffect(
          new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false, true));
    } else {
      living.removePotionEffect(PotionEffectType.GLOWING);
    }
  }

  public static void despawn(BoardCreature creature) {
    Entity body = findEntity(creature.entityId());
    if (body != null && body.getVehicle() != null) {
      body.getVehicle().remove();
    }
    if (body instanceof LivingEntity living && living.getPassengers() != null) {
      for (Entity passenger : living.getPassengers()) {
        passenger.remove();
      }
    }
    removeIfPresent(creature.entityId());
    removeIfPresent(creature.displayEntityId());
    creature.clearEntityRefs();
  }

  private static SpawnBundle trySpawnLiving(BoardCreature creature, Location spawnAt) {
    World world = spawnAt.getWorld();
    if (world == null) {
      return null;
    }

    try {
      LivingEntity mount = null;
      if (creature.template().hasMount()) {
        EntityType mountType = creature.template().mountEntityType();
        mount = spawnLiving(world, spawnAt, mountType);
        if (mount == null) {
          return null;
        }
      }

      Location riderAt = spawnAt.clone();
      if (mount != null) {
        riderAt.add(0, Math.max(0.1, mount.getHeight() * 0.35), 0);
      }

      EntityType bodyType = creature.template().entityType();
      LivingEntity entity = spawnLiving(riderAt.getWorld(), riderAt, bodyType);
      if (entity == null) {
        if (mount != null) {
          mount.remove();
        }
        return null;
      }
      if (!entity.isValid()) {
        entity.remove();
        if (mount != null) {
          mount.remove();
        }
        return null;
      }
      entity.setRotation(spawnAt.getYaw(), spawnAt.getPitch());
      MatchEntityProtection.apply(entity);
      if (mount != null && mount instanceof Mob mountMob) {
        mountMob.addPassenger(entity);
      }
      MatchEntityDisplay.tagCreature(entity, creature.instanceId());

      TextDisplay label = MatchEntityDisplay.spawnLabel(
          spawnAt.clone().add(0, entity.getHeight() + LABEL_HEIGHT, 0), buildLabel(creature));
      if (label == null) {
        entity.remove();
        if (mount != null) {
          mount.remove();
        }
        return null;
      }
      return new SpawnBundle(entity.getUniqueId(), label.getUniqueId());
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static SpawnBundle spawnFallback(BoardCreature creature, Location spawnAt) {
    BlockDisplay potato = MatchEntityDisplay.spawnPotatoModel(spawnAt);
    if (potato == null) {
      return null;
    }
    MatchEntityDisplay.tagCreature(potato, creature.instanceId());
    TextDisplay label = MatchEntityDisplay.spawnLabel(spawnAt, buildLabel(creature));
    if (label == null) {
      potato.remove();
      return null;
    }
    return new SpawnBundle(potato.getUniqueId(), label.getUniqueId());
  }

  /**
   * 使用 {@link SpawnReason#CUSTOM}，避免世界为和平难度时插件召唤被拦截。
   */
  private static LivingEntity spawnLiving(World world, Location at, EntityType type) {
    if (type == null || !type.isSpawnable() || !type.isAlive()) {
      return null;
    }
    Class<? extends Entity> entityClass = type.getEntityClass();
    if (entityClass == null || !LivingEntity.class.isAssignableFrom(entityClass)) {
      return null;
    }
  @SuppressWarnings("unchecked")
    Class<? extends LivingEntity> livingClass = (Class<? extends LivingEntity>) entityClass;
    try {
      return world.spawn(at, livingClass, SpawnReason.CUSTOM, entity -> MatchEntityProtection.apply(entity));
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static Component buildLabel(BoardCreature creature) {
    String line1 = "<white><bold>%s".formatted(creature.displayName());
    String line2 =
        "<red>生命 %d  <gold>力量 %d".formatted(creature.health(), creature.currentPower());
    String line3 =
        "<dark_purple>印记: <light_purple>%s".formatted(SigilNames.join(creature.activeSigils()));
    return TextParser.parseNonItalic(line1 + "\n" + line2 + "\n" + line3);
  }

  private static void removeIfPresent(UUID id) {
    if (id == null) return;
    Entity entity = findEntity(id);
    if (entity != null) entity.remove();
  }

  private static Entity findEntity(UUID id) {
    for (var world : Bukkit.getWorlds()) {
      Entity entity = world.getEntity(id);
      if (entity != null) return entity;
    }
    return null;
  }

  /** @deprecated 使用 {@link #purgeAllPluginEntities()} */
  @Deprecated
  public static void purgeAllMatchCreatures() {
    purgeAllPluginEntities();
  }

  /**
   * 仅清除对局造物/商人/飘字等遗留，<b>不</b>动场地玻璃踏板（{@link InscriptionKeys#ARENA_ID}）。
   */
  public static int purgeOrphanBoardEntities() {
    int removed = 0;
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        var pdc = entity.getPersistentDataContainer();
        if (pdc.has(InscriptionKeys.CREATURE_INSTANCE, PersistentDataType.STRING)
                || pdc.has(InscriptionKeys.WANDERING_TRADER, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.MATCH_DISPLAY, PersistentDataType.BYTE)
                || pdc.has(InscriptionKeys.DROP_CARD, PersistentDataType.STRING)) {
          entity.remove();
          removed++;
        }
      }
    }
    return removed;
  }

  /** 清除全服带邪恶冥刻标记的实体（含场地踏板、大厅装饰）；仅用于关服清理。 */
  public static int purgeAllPluginEntities() {
    int removed = 0;
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        if (!MatchEntityProtection.isProtectedMatchEntity(entity)
                && !entity.getPersistentDataContainer()
                        .has(InscriptionKeys.DROP_CARD, PersistentDataType.STRING)) {
          continue;
        }
        entity.remove();
        removed++;
      }
    }
    return removed;
  }

  private record SpawnBundle(UUID bodyId, UUID labelId) {}
}
