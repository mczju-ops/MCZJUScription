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

  /** 同列攻击者对该列造成的预测（单列攻击 / 空袭等）。 */
  public static PredictedStrike predictLane(
      InscriptionMatch match, MatchSide attackerSide, int laneIndex) {
    SlotOwner attackerOwner = toOwner(attackerSide);
    BoardSlot attackerSlot = match.board().slot(attackerOwner, laneIndex);
    if (attackerSlot.isEmpty()) return PredictedStrike.NONE;
    BoardCreature attacker = attackerSlot.creature();

    if (attacker.hasSigil(SigilId.ALL_STRIKE)) {
      return allStrikePlan(match, attackerSide);
    }
    if (attacker.hasSigil(SigilId.TRI_STRIKE) || attacker.hasSigil(SigilId.SPLIT_STRIKE)) {
      return PredictedStrike.NONE;
    }
    return predictSingleLane(match, attackerSide, laneIndex, attacker);
  }

  private static PredictedStrike allStrikePlan(InscriptionMatch match, MatchSide attackerSide) {
    SlotOwner defenderOwner = toOwner(attackerSide.opposite());
    for (BoardSlot slot : match.board().row(defenderOwner)) {
      if (!slot.isEmpty()) return PredictedStrike.NONE;
    }
    int damage =
        firstAttackerWith(match, attackerSide, SigilId.ALL_STRIKE)
            .map(a -> a.currentAttack() + CombatModifiers.leaderBonus(match.board(), a))
            .orElse(0);
    return damage > 0 ? PredictedStrike.DIRECT : PredictedStrike.NONE;
  }

  private static PredictedStrike predictSingleLane(
      InscriptionMatch match,
      MatchSide attackerSide,
      int laneIndex,
      BoardCreature attacker) {
    SlotOwner defenderOwner = toOwner(attackerSide.opposite());
    BoardSlot defenderSlot = match.board().slot(defenderOwner, laneIndex);
    BoardCreature defender = defenderSlot.isEmpty() ? null : defenderSlot.creature();
    boolean submerged =
        defender != null && CombatModifiers.isSubmerged(defender, attackerSide);

    if (defender != null && CombatModifiers.preventsAttack(defender)) {
      return PredictedStrike.NONE;
    }

    if (CombatModifiers.mustFightDefender(attacker, defender, submerged)) {
      int atk = CombatModifiers.effectiveAttack(match.board(), attacker, defender);
      return atk > 0 ? PredictedStrike.CREATURE : PredictedStrike.NONE;
    }

    if (defender == null || submerged || attacker.hasSigil(SigilId.AIR_STRIKE)) {
      int damage =
          attacker.currentAttack() + CombatModifiers.leaderBonus(match.board(), attacker);
      return damage > 0 ? PredictedStrike.DIRECT : PredictedStrike.NONE;
    }
    return PredictedStrike.NONE;
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
      if (attacker.hasSigil(SigilId.ALL_STRIKE)) {
        if (wouldMeleeStrike(match, attackerSide, attacker, defender)) return true;
        continue;
      }
      if (attacker.hasSigil(SigilId.TRI_STRIKE)) {
        if (Math.abs(atkLane - defLane) <= 1 && wouldMeleeStrike(match, attackerSide, attacker, defender)) {
          return true;
        }
        continue;
      }
      if (attacker.hasSigil(SigilId.SPLIT_STRIKE)) {
        if (Math.abs(atkLane - defLane) == 1 && wouldMeleeStrike(match, attackerSide, attacker, defender)) {
          return true;
        }
        continue;
      }
      if (atkLane == defLane && wouldMeleeStrike(match, attackerSide, attacker, defender)) {
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
    boolean submerged = CombatModifiers.isSubmerged(defender, attackerSide);
    if (CombatModifiers.preventsAttack(defender)) return false;
    if (!CombatModifiers.mustFightDefender(attacker, defender, submerged)) return false;
    return CombatModifiers.effectiveAttack(match.board(), attacker, defender) > 0;
  }

  private static SlotOwner toOwner(MatchSide side) {
    return side == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
  }

  private static java.util.Optional<BoardCreature> firstAttackerWith(
      InscriptionMatch match, MatchSide attackerSide, SigilId sigil) {
    SlotOwner owner = toOwner(attackerSide);
    for (BoardSlot slot : match.board().row(owner)) {
      if (slot.isEmpty()) continue;
      BoardCreature c = slot.creature();
      if (c.hasSigil(sigil)) return java.util.Optional.of(c);
    }
    return java.util.Optional.empty();
  }
}
