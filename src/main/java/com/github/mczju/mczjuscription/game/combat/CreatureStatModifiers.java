package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.BoardTokens;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import org.jetbrains.annotations.Nullable;

/** 印记对造物攻击/力量等属性的动态修正（含名牌展示与战斗结算）。 */
public final class CreatureStatModifiers {

  private CreatureStatModifiers() {}

  public static int auraPowerDelta(@Nullable BattleBoard board, BoardCreature creature) {
    if (board == null || creature.slot() == null) {
      return 0;
    }
    return scorchBonus(creature)
        + tauntAuraBonus(board)
        + ridingBonus(board, creature)
        + stinkyPenalty(board, creature);
  }

  public static int effectivePower(@Nullable BattleBoard board, BoardCreature creature) {
    return Math.max(0, creature.currentPower() + auraPowerDelta(board, creature));
  }

  public static int effectiveAttack(
      @Nullable BattleBoard board, BoardCreature attacker, BoardCreature defender) {
    return effectivePower(board, attacker);
  }

  /** 炽热：持印造物攻击 +1。 */
  private static int scorchBonus(BoardCreature creature) {
    return creature.hasSigil(SigilId.SCORCH) ? 1 : 0;
  }

  /** 嘲讽：全场（含敌我）造物 +1 力量。 */
  private static int tauntAuraBonus(BattleBoard board) {
    for (SlotOwner owner : SlotOwner.values()) {
      for (BoardSlot slot : board.row(owner)) {
        if (slot.creature() != null && slot.creature().hasSigil(SigilId.TAUNT_AURA)) {
          return 1;
        }
      }
    }
    return 0;
  }

  /** 骑乘：相邻友方 +1 力量。 */
  private static int ridingBonus(BattleBoard board, BoardCreature creature) {
    BoardSlot slot = creature.slot();
    if (slot == null) return 0;
    SlotOwner owner = creature.owner() == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
    BoardSlot[] row = board.row(owner);
    int idx = slot.index();
    int bonus = 0;
    for (int delta : new int[] {-1, 1}) {
      int i = idx + delta;
      if (i < 0 || i >= BoardSlot.SLOT_COUNT) continue;
      BoardCreature neighbor = row[i].creature();
      if (neighbor != null && neighbor.hasSigil(SigilId.RIDING)) {
        bonus++;
      }
    }
    return bonus;
  }

  /** 臭臭：正对面同列敌方造物力量 -1。 */
  private static int stinkyPenalty(BattleBoard board, BoardCreature creature) {
    int penalty = 0;
    for (MatchSide side : MatchSide.values()) {
      for (BoardSlot slot : board.occupiedSlots(side)) {
        BoardCreature source = slot.creature();
        if (source == null || source == creature) continue;
        if (!source.hasSigil(SigilId.STINKY) && !source.hasSigil(SigilId.STINKY_FAR)) {
          continue;
        }
        BoardCreature opposite = BoardTokens.oppositeInLane(board, source);
        if (opposite == creature) {
          penalty--;
        }
      }
    }
    return penalty;
  }
}
