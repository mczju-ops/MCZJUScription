package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardShift;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

/** 【游荡】：回合结束后向相邻随机空位走去（非攻击后位移）。 */
public final class WanderHandler {

  private WanderHandler() {}

  public static void wander(InscriptionMatch match, BoardCreature source) {
    if (!source.hasSigil(SigilId.WANDER)) return;
    BoardShift.moveRandomAdjacent(match, source, BoardShift.ShiftStyle.WALK);
  }
}
