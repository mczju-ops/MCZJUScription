package com.github.mczju.mczjuscription.roguelike;

import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.entity.MatchEntityDisplay;
import com.github.mczju.mczjuscription.entity.MatchEntityProtection;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.menu.WanderingTraderMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;

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

  public static void spawnTrader(InscriptionMatch match) {
    despawnTrader(match);
    Location at = resolveTraderLocation(match);
    if (at == null || at.getWorld() == null) {
      match.feedback().actionBarWarn("<yellow>未配置商人位置（房间 traderAt 或场地钟附近）");
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
      match.feedback().actionBarWarn("<yellow>商人即将离开");
    }
    new WanderingTraderMenu(player, match, side).open();
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
      WanderingTrader trader = (WanderingTrader) at.getWorld().spawnEntity(at, EntityType.WANDERING_TRADER);
      MatchEntityProtection.apply(trader);
      trader.setCustomNameVisible(true);
      trader.customName(Component.text("流浪商人"));
      MatchEntityDisplay.tagWanderingTrader(trader);
      return trader;
    } catch (RuntimeException ignored) {
      // 部分世界/版本无法生成流浪商人，走展示实体保底
    }
    try {
      LivingEntity villager = (LivingEntity) at.getWorld().spawnEntity(at, EntityType.VILLAGER);
      MatchEntityProtection.apply(villager);
      villager.setCustomNameVisible(true);
      villager.customName(Component.text("流浪商人"));
      MatchEntityDisplay.tagWanderingTrader(villager);
      return villager;
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static Location resolveTraderLocation(InscriptionMatch match) {
    InscriptionGameRoom room = match.matchRoom();
    if (room != null && room.traderAt != null) {
      Location at = room.traderAt.clone();
      if (room.traderYaw != null) {
        at.setYaw(room.traderYaw);
      }
      return at;
    }
    BattleArena arena = match.arena();
    if (arena != null) {
      if (arena.hasClock()) {
        return arena.clockLocation();
      }
      Location slot = arena.slotLocation(SlotOwner.PLAYER, 1);
      if (slot != null) {
        return ArenaFacing.withYawToward(
            slot, ArenaFacing.facingTarget(arena, SlotOwner.ENEMY, 1));
      }
    }
    if (room != null && room.spawnAt != null) {
      return room.spawnAt.clone();
    }
    return null;
  }
}
