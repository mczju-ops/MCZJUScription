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
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public final class CreatureEntityService {

  private static final double LABEL_HEIGHT = 0.35;

  private CreatureEntityService() {}

  public static void spawn(BoardCreature creature, Location at) {
    CardTemplate def = creature.template();
    Location spawnAt = at.clone();

    LivingEntity mount = null;
    if (def.hasMount()) {
      mount = (LivingEntity) spawnAt.getWorld().spawnEntity(spawnAt, def.mountEntityType());
      configureMob(mount);
    }

    Location riderAt = spawnAt.clone();
    if (mount != null) {
      riderAt.add(0, Math.max(0.1, mount.getHeight() * 0.35), 0);
    }

    LivingEntity entity =
        (LivingEntity) riderAt.getWorld().spawnEntity(riderAt, def.entityType());
    entity.setRotation(spawnAt.getYaw(), spawnAt.getPitch());
    configureMob(entity);
    if (mount != null && mount instanceof org.bukkit.entity.Mob mountMob) {
      mountMob.addPassenger(entity);
    }
    entity
        .getPersistentDataContainer()
        .set(
            InscriptionKeys.CREATURE_INSTANCE,
            PersistentDataType.STRING,
            creature.instanceId().toString());

    TextDisplay label =
        spawnAt
            .getWorld()
            .spawn(
                spawnAt.clone().add(0, entity.getHeight() + LABEL_HEIGHT, 0),
                TextDisplay.class,
                display -> {
                  display.text(buildLabel(creature));
                  display.setBillboard(Display.Billboard.CENTER);
                  display.setSeeThrough(true);
                  display.setShadowed(true);
                  display.setDefaultBackground(false);
                  display.setPersistent(true);
                });

    creature.bindEntity(entity.getUniqueId(), label.getUniqueId());
  }

  public static void refreshLabel(BoardCreature creature) {
    UUID displayId = creature.displayEntityId();
    if (displayId == null) return;
    Entity entity = findEntity(displayId);
    if (entity instanceof TextDisplay display) {
      display.text(buildLabel(creature));
    }
  }

  public static void despawn(BoardCreature creature) {
    Entity ridden = findEntity(creature.entityId());
    if (ridden != null && ridden.getVehicle() != null) {
      ridden.getVehicle().remove();
    }
    removeIfPresent(creature.entityId());
    removeIfPresent(creature.displayEntityId());
    creature.clearEntityRefs();
  }

  private static void configureMob(LivingEntity entity) {
    entity.setCustomNameVisible(false);
    entity.setRemoveWhenFarAway(false);
    entity.setPersistent(true);
    if (entity instanceof Mob mob) {
      mob.setAI(false);
      mob.setAware(false);
      mob.setCollidable(false);
      mob.setSilent(true);
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

  public static void purgeAllMatchCreatures() {
    for (World world : Bukkit.getWorlds()) {
      for (Entity entity : world.getEntities()) {
        if (!entity.getPersistentDataContainer()
            .has(InscriptionKeys.CREATURE_INSTANCE, PersistentDataType.STRING)) {
          continue;
        }
        entity.remove();
      }
    }
  }
}
