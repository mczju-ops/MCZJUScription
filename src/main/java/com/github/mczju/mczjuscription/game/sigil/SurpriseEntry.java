package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/** 【意外】：获得该卡时若场上有空位则免费召唤。 */
public final class SurpriseEntry {

  private SurpriseEntry() {}

  public static void tryAutoSummon(InscriptionMatch match, MatchSide side, String templateId) {
    if (!CardCatalog.require(templateId).hasSigil(SigilId.SURPRISE_ENTRY)) return;
    if (match.arena() == null) return;
    SlotOwner owner = BoardSides.toSlotOwner(side);
    int slot = BoardRules.nextEmptyIndex(match.board().row(owner), 0);
    if (slot < 0) return;
    match.forcePlayFromHand(side, templateId, slot);
  }
}
