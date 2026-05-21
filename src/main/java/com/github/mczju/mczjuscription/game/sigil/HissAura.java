package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/** 【哈气】：对面场上有豹猫时，己方【自爆】无效。 */
public final class HissAura {

  private HissAura() {}

  public static boolean suppressesSelfDestruct(InscriptionMatch match, MatchSide bomberSide) {
    MatchSide enemy = bomberSide.opposite();
    for (BoardSlot slot : match.board().occupiedSlots(enemy)) {
      BoardCreature c = slot.creature();
      if (c != null && c.hasSigil(SigilId.HISS)) {
        return true;
      }
    }
    return false;
  }
}
