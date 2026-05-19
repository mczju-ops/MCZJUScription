package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/** 食尸鬼：战斗中己方造物阵亡时，手牌中的食尸鬼自动填入首个空槽。 */
public final class CorpseEaterHandler {

  private CorpseEaterHandler() {}

  public static void tryAutoplayAfterAllyDeath(InscriptionMatch match, MatchSide fallenSide) {
    if (!match.isCombatAnimating()) return;

    var state = match.participant(fallenSide);
    String eater = null;
    for (String inHand : state.hand()) {
      if (CardCatalog.require(inHand).hasSigil(SigilId.CORPSE_EATER)) {
        eater = inHand;
        break;
      }
    }
    if (eater == null) return;

    SlotOwner owner = BoardSides.toSlotOwner(fallenSide);
    int slot = BoardRules.nextEmptyIndex(match.board().row(owner), 0);
    if (slot < 0) return;

    match.forcePlayFromHand(fallenSide, eater, slot);
  }
}
