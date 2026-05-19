package com.github.mczju.mczjuscription.roguelike;

import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.menu.WanderingTraderMenu;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

/** 第 3、6、9… 回合出现的流浪商人。 */
public final class WanderingTraderService {

  private WanderingTraderService() {}

  public static boolean isTraderTurn(int turnNumber) {
    return turnNumber > 0 && turnNumber % 3 == 0;
  }

  public static void onTraderTurnStart(InscriptionMatch match, InscriptionGameRoom room) {
    if (!isTraderTurn(match.turn().turnNumber())) return;
    spawnTrader(match, room);
    match
        .feedback()
        .announceInfo("<gold>流浪商人来到了场上！<gray>前往商人处右键交互。");
    for (var human : match.humanParticipants()) {
      human
          .player()
          .ifPresent(
              ext ->
                  ext.player()
                      .sendMessage(
                          "§6[流浪商人] §7印制印记 §c×%d§7 腐肉，融合两张卡 §c×%d§7 腐肉。"
                              .formatted(TraderCosts.IMPRINT_BLOOD, TraderCosts.FUSION_BLOOD)));
    }
  }

  public static void spawnTrader(InscriptionMatch match, InscriptionGameRoom room) {
    if (room == null || room.traderAt == null) return;
    despawnTrader(match);
    Location at = room.traderAt.clone();
    if (room.traderYaw != null) {
      at.setYaw(room.traderYaw);
    }
    Location spawn = at;
    Villager villager = (Villager) spawn.getWorld().spawnEntity(spawn, EntityType.VILLAGER);
    villager.setAI(false);
    villager.setInvulnerable(true);
    villager.setSilent(true);
    villager.setRemoveWhenFarAway(false);
    villager.setPersistent(false);
    villager.setCustomNameVisible(true);
    villager.customName(Component.text("流浪商人"));
    villager.setProfession(Villager.Profession.CARTOGRAPHER);
    match.setWanderingTraderEntityId(villager.getUniqueId());
  }

  public static void despawnTrader(InscriptionMatch match) {
    java.util.UUID id = match.wanderingTraderEntityId();
    if (id == null) return;
    for (var world : org.bukkit.Bukkit.getWorlds()) {
      Entity e = world.getEntity(id);
      if (e != null) e.remove();
    }
    match.setWanderingTraderEntityId(null);
  }

  public static void tryOpenMenu(Player player, InscriptionMatch match) {
    MatchSide side = match.sideFor(player);
    if (side == null) return;
    if (!isTraderTurn(match.turn().turnNumber())) {
      match.feedback().actionBarWarn("<yellow>商人即将离开");
    }
    new WanderingTraderMenu(player, match, side).open();
  }
}
