package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.combat.CreatureStatModifiers;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import com.github.mczjuops.mczjugamecore.utils.TextParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class CreatureEntityService {

  private static final double LABEL_HEIGHT = 0.35;

  private CreatureEntityService() {}

  public static void spawn(BoardCreature creature, Location at) {
    Location spawnAt = CreatureBoardOrientation.applyBoardStand(creature, at);
    if (spawnAt == null || spawnAt.getWorld() == null) {
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
    if (creature.willSkipNextAttack()) {
      com.github.mczju.mczjuscription.vfx.InkAuraVfx.start(creature);
    }
  }

  public static void refreshLabel(BoardCreature creature) {
    BattleBoard board = creature.slot() != null ? creature.slot().board() : null;
    refreshLabel(board, creature);
  }

  public static void refreshLabel(@Nullable BattleBoard board, BoardCreature creature) {
    UUID displayId = creature.displayEntityId();
    if (displayId == null) return;
    Entity entity = findEntity(displayId);
    if (entity instanceof TextDisplay display) {
      display.text(buildLabel(board, creature));
    }
  }

  public static void refreshBoardLabels(BattleBoard board) {
    if (board == null) return;
    for (SlotOwner owner : SlotOwner.values()) {
      for (BoardSlot slot : board.row(owner)) {
        if (slot.isEmpty()) continue;
        refreshLabel(board, slot.creature());
      }
    }
  }

  /** 将造物实体对齐到当前逻辑槽位（与 spawn 朝向一致）。 */
  public static void snapToBoardSlot(InscriptionMatch match, BoardCreature creature) {
    if (match == null || creature == null || match.arena() == null) return;
    BoardSlot slot = creature.slot();
    if (slot == null) return;
    Location center = match.arena().slotLocation(slot.owner(), slot.index());
    if (center == null) return;
    snapToBoardSlot(match, creature, slot.owner(), slot.index(), center);
  }

  /** 对齐到指定槽位中心（推挤动画前先把被推挤者钉在被推格）。 */
  public static void snapToBoardSlot(
      InscriptionMatch match,
      BoardCreature creature,
      SlotOwner owner,
      int index,
      Location slotCenter) {
    if (match == null || creature == null || slotCenter == null || match.arena() == null) {
      return;
    }
    Location faceTarget = ArenaFacing.facingTarget(match.arena(), owner, index);
    Location stand =
        CreatureAnimator.slotStand(
            creature, ArenaFacing.withYawToward(slotCenter, faceTarget));
    if (stand == null) return;
    if (faceTarget != null) {
      stand.setYaw(ArenaFacing.yawFacing(stand, faceTarget));
      LivingEntity mob = findLiving(creature);
      if (mob != null) {
        stand.setPitch(CreatureBoardOrientation.boardPitch(mob));
      }
    }
    CreatureAnimator.snapToStand(creature, stand);
  }

  private static LivingEntity findLiving(BoardCreature creature) {
    if (creature.entityId() == null) return null;
    Entity entity = findEntity(creature.entityId());
    return entity instanceof LivingEntity living ? living : null;
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
    com.github.mczju.mczjuscription.vfx.InkAuraVfx.stop(creature);
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
      CreatureBoardScale.apply(entity, bodyType);
      CreatureBoardOrientation.applySpawnPose(entity, creature, spawnAt);
      MatchEntityProtection.apply(entity);
      if (mount != null && mount instanceof Mob mountMob) {
        mountMob.addPassenger(entity);
      }
      MatchEntityDisplay.tagCreature(entity, creature.instanceId());

      TextDisplay label = MatchEntityDisplay.spawnLabel(
          spawnAt.clone().add(0, entity.getHeight() + LABEL_HEIGHT, 0), buildLabel(null, creature));
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
    Location stand = CreatureBoardOrientation.applyBoardStand(creature, spawnAt);
    if (stand == null) {
      return null;
    }
    BlockDisplay potato = MatchEntityDisplay.spawnPotatoModel(stand);
    if (potato == null) {
      return null;
    }
    MatchEntityDisplay.tagCreature(potato, creature.instanceId());
    TextDisplay label = MatchEntityDisplay.spawnLabel(stand, buildLabel(null, creature));
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
      LivingEntity spawned =
          world.spawn(
              at,
              livingClass,
              SpawnReason.CUSTOM,
              entity -> MatchEntityProtection.apply(entity));
      if (spawned instanceof Boss) {
        MCZJUScriptionPlugin plugin = MCZJUScriptionPlugin.getInstance();
        if (plugin != null) {
          Bukkit.getScheduler()
              .runTaskLater(plugin, () -> MatchEntityProtection.hideBossBar(spawned), 1L);
        }
      }
      return spawned;
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static Component buildLabel(@Nullable BattleBoard board, BoardCreature creature) {
    String line1 = "<white><bold>%s".formatted(creature.displayName());
    int powerBaseline = creature.template().power();
    int effectivePower = CreatureStatModifiers.effectivePower(board, creature);
    int healthBaseline = healthBaseline(creature);
    String line2 =
        "<red>生命 %s  <gold>力量 %s"
            .formatted(formatHealthNumber(creature), formatStatNumber(effectivePower, powerBaseline));
    String line3 =
        "<dark_purple>印记: <light_purple>%s".formatted(SigilNames.join(creature.activeSigils()));
    return TextParser.parseNonItalic(line1 + "\n" + line2 + "\n" + line3);
  }

  private static int healthBaseline(BoardCreature creature) {
    int baseline = creature.template().health();
    if (creature.hasMaturedFromFledgling()) {
      baseline += 1;
    }
    return baseline;
  }

  private static String formatHealthNumber(BoardCreature creature) {
    int baseline = healthBaseline(creature);
    int current = creature.health();
    if (current > baseline) {
      return "<blue>%d".formatted(current);
    }
    if (current < baseline) {
      return "<gold>%d".formatted(current);
    }
    return "<gold>%d".formatted(current);
  }

  /** 相对卡牌基础值：变低红色，变高蓝色，不变金色。 */
  private static String formatStatNumber(int value, int baseline) {
    if (value > baseline) {
      return "<blue>%d".formatted(value);
    }
    if (value < baseline) {
      return "<red>%d".formatted(value);
    }
    return "<gold>%d".formatted(value);
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
