package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilId;

/** 战斗中的印记修正。 */
public final class CombatModifiers {

  private CombatModifiers() {}

  public static boolean isSubmerged(
      BattleBoard board, BoardCreature defender, MatchSide attackerSide) {
    if (!defender.hasSigil(SigilId.WATER_STRIKE) || defender.owner() == attackerSide) {
      return false;
    }
    return !attackerHasSonar(board, attackerSide);
  }

  private static boolean attackerHasSonar(BattleBoard board, MatchSide attackerSide) {
    for (BoardSlot slot : board.occupiedSlots(attackerSide)) {
      if (slot.creature() != null && slot.creature().hasSigil(SigilId.SONAR)) {
        return true;
      }
    }
    return false;
  }

  public static boolean mustFightDefender(
      BoardCreature attacker, BoardCreature defender, boolean submerged) {
    if (defender == null || submerged) return false;
    if (!attacker.hasSigil(SigilId.AIR_STRIKE)) return true;
    return defender.hasSigil(SigilId.HIGH_JUMP);
  }

  public static boolean preventsAttack(BoardCreature defender) {
    return defender.hasSigil(SigilId.INTIMIDATE);
  }

  public static boolean venomKill(BoardCreature attacker, BoardCreature defender) {
    if (!attacker.hasSigil(SigilId.VENOM_KILL)) return false;
    return !defender.hasSigil(SigilId.HARD_SHELL);
  }

  /** @deprecated use {@link #venomKill} */
  public static boolean canDeathtouchKill(BoardCreature attacker, BoardCreature defender) {
    return venomKill(attacker, defender);
  }

  public static int capIncomingDamage(BoardCreature defender, int damage) {
    if (defender.hasSigil(SigilId.HARD_SHELL)) {
      return Math.min(damage, 1);
    }
    return damage;
  }

  public static int effectiveAttack(
      BattleBoard board, BoardCreature attacker, BoardCreature defender) {
    int atk =
        attacker.currentAttack()
            + ridingBonus(board, attacker)
            + tauntAuraBonus(board)
            + scorchBonus(attacker);
    if (defender != null && attacker.owner() != defender.owner()) {
      if (defender.hasSigil(SigilId.STINKY)) {
        atk = Math.max(0, atk - 1);
      }
      atk = Math.max(0, atk + stinkyFarPenalty(board, attacker.owner()));
    }
    return Math.max(0, atk);
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

  /** 炽热：持印造物攻击 +1。 */
  private static int scorchBonus(BoardCreature attacker) {
    return attacker.hasSigil(SigilId.SCORCH) ? 1 : 0;
  }

  /** 猪的臭臭：对方场上存在该印记时，己方攻击 -1。 */
  private static int stinkyFarPenalty(BattleBoard board, MatchSide attackerSide) {
    MatchSide enemy = attackerSide.opposite();
    for (BoardSlot slot : board.occupiedSlots(enemy)) {
      BoardCreature c = slot.creature();
      if (c != null && c.hasSigil(SigilId.STINKY_FAR)) {
        return -1;
      }
    }
    return 0;
  }

  /** 兼容旧调用：领袖力量已移除，返回 0。 */
  public static int leaderBonus(BattleBoard board, BoardCreature creature) {
    return ridingBonus(board, creature) + tauntAuraBonus(board);
  }
}
