package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/**
 * 场面 token 放置：筑坝师、鸣钟人等「邻格空位」产出。
 */
public final class BoardTokens {

  private BoardTokens() {}

  /** 在指定侧某列放置 token（格须为空）。 */
  public static boolean spawnTokenOnSlot(
      InscriptionMatch match, MatchSide side, int slotIndex, String templateId) {
    if (match.arena() == null) return false;
    SlotOwner owner = BoardSides.toSlotOwner(side);
    BoardSlot[] row = match.board().row(owner);
    if (!BoardRules.canPlaceAt(row, slotIndex)) return false;
    BoardSlot slot = match.board().slot(owner, slotIndex);
    BoardCreature token = new BoardCreature(templateId, side);
    token.bind(slot);
    match.spawnCreatureEntity(token, owner, slotIndex);
    return true;
  }

  /**
   * 以 {@code source} 所在格为中心，在**同排左右**相邻空位放置 token（不含自身格）。
   */
  public static void spawnOnAdjacentEmpty(
      InscriptionMatch match, BoardCreature source, String templateId) {
    BoardSlot sourceSlot = source.slot();
    if (sourceSlot == null) return;
    int center = sourceSlot.index();
    MatchSide side = source.owner();
    for (int delta : new int[] {-1, 1}) {
      int idx = center + delta;
      if (idx < 0 || idx >= BoardSlot.SLOT_COUNT) continue;
      spawnTokenOnSlot(match, side, idx, templateId);
    }
  }

  public static void spawnOnAdjacentEmpty(
      InscriptionMatch match, MatchSide side, int centerIndex, String templateId) {
    for (int delta : new int[] {-1, 1}) {
      int idx = centerIndex + delta;
      if (idx < 0 || idx >= BoardSlot.SLOT_COUNT) continue;
      spawnTokenOnSlot(match, side, idx, templateId);
    }
  }

  /** 对面同列槽位上的造物（铁兽夹等）。 */
  public static BoardCreature oppositeInLane(
      BattleBoard board, BoardCreature source) {
    BoardSlot slot = source.slot();
    if (slot == null) return null;
    SlotOwner opposite =
        source.owner() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    BoardSlot target = board.slot(opposite, slot.index());
    return target.isEmpty() ? null : target.creature();
  }
}
