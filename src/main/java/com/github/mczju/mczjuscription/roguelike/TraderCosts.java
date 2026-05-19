package com.github.mczju.mczjuscription.roguelike;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import org.bukkit.entity.Player;

/** 流浪商人服务腐肉（鲜血）费用。 */
public final class TraderCosts {

  public static final int IMPRINT_BLOOD = 2;
  public static final int FUSION_BLOOD = 4;

  private TraderCosts() {}

  /** 扣除腐肉并刷新资源栏；不足时提示玩家。 */
  public static boolean trySpendBlood(
      InscriptionMatch match, MatchSide side, Player player, int cost) {
    if (!match.currency(side).trySpendBlood(cost)) {
      player.sendMessage("§c腐肉不足（需要 ×" + cost + "）");
      match.feedback().actionBarWarn("<red>腐肉不足 ×%d".formatted(cost));
      return false;
    }
    match.syncHud();
    return true;
  }
}
