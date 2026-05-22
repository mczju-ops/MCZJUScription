package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
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
    return CreatureStatModifiers.effectiveAttack(board, attacker, defender);
  }

  /** 兼容旧调用：领袖力量已移除，返回 0。 */
  public static int leaderBonus(BattleBoard board, BoardCreature creature) {
    return 0;
  }
}
