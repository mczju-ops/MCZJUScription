package com.github.mczju.mczjuscription.roguelike;

import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.entity.MatchEntityDisplay;
import com.github.mczju.mczjuscription.entity.MatchEntityProtection;
import com.github.mczju.mczjuscription.arena.ResolvedArenaLayout;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.menu.WanderingTraderMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Merchant;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import java.util.Collections;

/** 第 3、6、9… 回合出现的流浪商人。 */
public final class WanderingTraderService {

  private WanderingTraderService() {}

  public static boolean isTraderTurn(int turnNumber) {
    return turnNumber > 0 && turnNumber % 3 == 0;
  }

  public static void onTraderTurnStart(InscriptionMatch match) {
    if (!isTraderTurn(match.turn().turnNumber())) {
      return;
    }
    spawnTrader(match);
    match.feedback().announceInfo("<gold>流浪商人来到了场上！<gray>前往商人处右键交互。");
    for (var human : match.humanParticipants()) {
      human.player()
          .ifPresent(
              ext ->
                  ext.player()
                      .sendMessage(
                          "§6[流浪商人] §7印制印记 §c×%d§7 腐肉，融合两张卡 §c×%d§7 腐肉。"
                              .formatted(TraderCosts.IMPRINT_BLOOD, TraderCosts.FUSION_BLOOD)));
    }
  }

  /** 抽牌阶段进入出牌阶段时补生成（若尚未存在），不重复广播。 */
  public static void ensureTraderPresent(InscriptionMatch match) {
    if (!isTraderTurn(match.turn().turnNumber())) {
      return;
    }
    if (match.wanderingTraderEntityId() != null) {
      Entity existing = findEntity(match.wanderingTraderEntityId());
      if (existing != null && existing.isValid()) {
        return;
      }
    }
    spawnTrader(match);
  }

  public static void spawnTrader(InscriptionMatch match) {
    despawnTrader(match);
    Location at = resolveTraderLocation(match);
    if (at == null || at.getWorld() == null) {
      match.feedback().actionBarWarn("<yellow>未配置流浪商人位置");
      return;
    }

    Entity spawned = trySpawnTraderMob(at);
    if (spawned == null) {
      spawned = MatchEntityDisplay.spawnPotatoModel(at);
      if (spawned != null) {
        MatchEntityDisplay.tagWanderingTrader(spawned);
      }
    }
    if (spawned == null) {
      match.feedback().actionBarWarn("<yellow>流浪商人生成失败");
      return;
    }
    match.setWanderingTraderEntityId(spawned.getUniqueId());
  }

  public static void despawnTrader(InscriptionMatch match) {
    java.util.UUID id = match.wanderingTraderEntityId();
    if (id == null) {
      return;
    }
    for (var world : org.bukkit.Bukkit.getWorlds()) {
      Entity e = world.getEntity(id);
      if (e != null) {
        e.remove();
      }
    }
    match.setWanderingTraderEntityId(null);
  }

  public static void tryOpenMenu(Player player, InscriptionMatch match) {
    MatchSide side = match.sideFor(player);
    if (side == null) {
      return;
    }
    if (!isTraderTurn(match.turn().turnNumber())) {
      match.feedback().actionBarWarn("<yellow>本回合没有流浪商人");
      return;
    }
    var phase = match.turn().phase();
    if (phase != com.github.mczju.mczjuscription.game.turn.TurnPhase.DRAW
            && phase != com.github.mczju.mczjuscription.game.turn.TurnPhase.PLAY) {
      match.feedback().actionBarWarn("<yellow>战斗阶段无法访问商人");
      return;
    }
    new WanderingTraderMenu(player, match, side).open();
  }

  private static Entity findEntity(java.util.UUID id) {
    for (var world : org.bukkit.Bukkit.getWorlds()) {
      Entity entity = world.getEntity(id);
      if (entity != null) {
        return entity;
      }
    }
    return null;
  }

  public static boolean isTraderEntity(Entity entity, InscriptionMatch match) {
    if (entity == null) {
      return false;
    }
    if (match.wanderingTraderEntityId() != null
        && entity.getUniqueId().equals(match.wanderingTraderEntityId())) {
      return true;
    }
    return MatchEntityDisplay.isWanderingTrader(entity);
  }

  private static Entity trySpawnTraderMob(Location at) {
    try {
      WanderingTrader trader =
              at.getWorld()
                      .spawn(at, WanderingTrader.class, SpawnReason.CUSTOM, entity -> configureTraderMob(entity));
      configureTraderMob(trader);
      trader.setCustomNameVisible(true);
      trader.customName(Component.text("流浪商人"));
      MatchEntityDisplay.tagWanderingTrader(trader);
      return trader;
    } catch (RuntimeException ignored) {
      // 部分世界/版本无法生成流浪商人，走展示实体保底
    }
    try {
      LivingEntity villager =
              at.getWorld()
                      .spawn(at, Villager.class, SpawnReason.CUSTOM, entity -> configureTraderMob(entity));
      configureTraderMob(villager);
      villager.setCustomNameVisible(true);
      villager.customName(Component.text("流浪商人"));
      MatchEntityDisplay.tagWanderingTrader(villager);
      return villager;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static void configureTraderMob(LivingEntity entity) {
    MatchEntityProtection.apply(entity);
    if (entity instanceof Merchant merchant) {
      merchant.setRecipes(Collections.emptyList());
    }
    if (entity instanceof Villager villager) {
      villager.setVillagerType(Villager.Type.PLAINS);
      villager.setProfession(Villager.Profession.NONE);
    }
  }

  private static Location resolveTraderLocation(InscriptionMatch match) {
    MatchSide side = match.actingSide();
    BattleArena arena = match.arena();
    if (arena != null && arena.layout() != null) {
      ResolvedArenaLayout.StagingSites staging = arena.layout().staging(side);
      if (staging != null && staging.wanderingTrader != null) {
        return staging.wanderingTrader.clone();
      }
      Location clock = arena.clockLocation(side);
      if (clock != null) {
        return clock.clone();
      }
    }
    return null;
  }
}
