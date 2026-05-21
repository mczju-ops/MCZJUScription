package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilId;

/** 战前预测：某列是否会直击、某造物是否将遭对战攻击。 */
public final class CombatTargeting {

  public enum PredictedStrike {
    NONE,
    CREATURE,
    DIRECT
  }

  private CombatTargeting() {}

  /**
   * 预测敌方第 {@code defenderLane} 列将受到的打击（用于守卫者挡刀等）。
   *
   * @param defenderLane 防守方列索引（0～3）
   */
  public static PredictedStrike predictLane(
      InscriptionMatch match, MatchSide attackerSide, int defenderLane) {
    SlotOwner defenderOwner = toOwner(attackerSide.opposite());
    BoardSlot defenderSlot = match.board().slot(defenderOwner, defenderLane);

    if (defenderSlot.isEmpty()) {
      if (anyAttackerDirectsOnLane(match, attackerSide, defenderLane)) {
        return PredictedStrike.DIRECT;
      }
      return PredictedStrike.NONE;
    }

    BoardCreature defender = defenderSlot.creature();
    if (defender != null && CombatModifiers.preventsAttack(defender)) {
      return PredictedStrike.NONE;
    }
    if (anyAttackerMeleeOnLane(match, attackerSide, defenderLane, defender)) {
      return PredictedStrike.CREATURE;
    }
    return PredictedStrike.NONE;
  }

  /** 多列印记或同列空袭：空列将吃直伤。 */
  private static boolean anyAttackerDirectsOnLane(
      InscriptionMatch match, MatchSide attackerSide, int defenderLane) {
    SlotOwner attackerOwner = toOwner(attackerSide);
    for (BoardSlot slot : match.board().row(attackerOwner)) {
      if (slot.isEmpty()) continue;
      BoardCreature attacker = slot.creature();
      if (!strikesDefenderLane(attacker, slot.index(), defenderLane)) continue;
      if (CombatModifiers.effectiveAttack(match.board(), attacker, null) > 0) {
        return true;
      }
    }
    return false;
  }

  private static boolean anyAttackerMeleeOnLane(
      InscriptionMatch match,
      MatchSide attackerSide,
      int defenderLane,
      BoardCreature defender) {
    SlotOwner attackerOwner = toOwner(attackerSide);
    for (BoardSlot slot : match.board().row(attackerOwner)) {
      if (slot.isEmpty()) continue;
      BoardCreature attacker = slot.creature();
      if (!strikesDefenderLane(attacker, slot.index(), defenderLane)) continue;
      if (wouldMeleeStrike(match, attackerSide, attacker, defender)) {
        return true;
      }
    }
    return false;
  }

  /** 该攻击者是否覆盖敌方指定列（全向 / 三路 / 两路 / 同列常规）。 */
  private static boolean strikesDefenderLane(
      BoardCreature attacker, int attackerLane, int defenderLane) {
    if (attacker.hasSigil(SigilId.ALL_STRIKE)) {
      return true;
    }
    if (attacker.hasSigil(SigilId.TRI_STRIKE)) {
      return Math.abs(attackerLane - defenderLane) <= 1;
    }
    if (attacker.hasSigil(SigilId.SPLIT_STRIKE)) {
      return Math.abs(attackerLane - defenderLane) == 1;
    }
    return attackerLane == defenderLane;
  }

  public static boolean willCreatureBeAttacked(
      InscriptionMatch match, MatchSide attackerSide, BoardCreature defender) {
    BoardSlot defSlot = defender.slot();
    if (defSlot == null) return false;
    int defLane = defSlot.index();
    SlotOwner attackerOwner = toOwner(attackerSide);

    for (int atkLane = 0; atkLane < BoardSlot.SLOT_COUNT; atkLane++) {
      BoardSlot atkSlot = match.board().slot(attackerOwner, atkLane);
      if (atkSlot.isEmpty()) continue;
      BoardCreature attacker = atkSlot.creature();
      if (!strikesDefenderLane(attacker, atkLane, defLane)) continue;
      if (wouldMeleeStrike(match, attackerSide, attacker, defender)) {
        return true;
      }
    }
    return false;
  }

  private static boolean wouldMeleeStrike(
      InscriptionMatch match,
      MatchSide attackerSide,
      BoardCreature attacker,
      BoardCreature defender) {
    if (defender.owner() == attacker.owner()) return false;
    boolean submerged = CombatModifiers.isSubmerged(match.board(), defender, attackerSide);
    if (CombatModifiers.preventsAttack(defender)) return false;
    if (!CombatModifiers.mustFightDefender(attacker, defender, submerged)) return false;
    return CombatModifiers.effectiveAttack(match.board(), attacker, defender) > 0;
  }

  private static SlotOwner toOwner(MatchSide side) {
    return side == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
  }
}
