package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BoardShift;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.BoardTokens;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 战前印记：钻地龙 / 守护者挡空列、断尾求生。 */
public final class PreCombatResolver {

  private PreCombatResolver() {}

  public static void resolve(InscriptionMatch match, MatchSide attackerSide) {
    blockEmptyLanes(match, attackerSide, SigilId.WHACK_A_MOLE);
    blockEmptyLanes(match, attackerSide, SigilId.GUARD_DOG);
    applyTailOnHit(match, attackerSide);
    match.syncHud();
  }

  private static void blockEmptyLanes(
      InscriptionMatch match, MatchSide attackerSide, SigilId sigil) {
    MatchSide defenderSide = attackerSide.opposite();
    SlotOwner defenderOwner = defenderSide == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;

    for (int lane = 0; lane < BoardSlot.SLOT_COUNT; lane++) {
      if (CombatTargeting.predictLane(match, attackerSide, lane)
          != CombatTargeting.PredictedStrike.DIRECT) {
        continue;
      }
      if (!match.board().slot(defenderOwner, lane).isEmpty()) {
        continue;
      }

      BoardCreature blocker = findBlocker(match, defenderSide, lane, sigil);
      if (blocker != null) {
        BoardShift.moveToIndex(match, blocker, lane);
      }
    }
  }

  private static BoardCreature findBlocker(
      InscriptionMatch match, MatchSide defenderSide, int targetLane, SigilId sigil) {
    List<BoardCreature> candidates = new ArrayList<>();
    for (BoardSlot slot : match.board().occupiedSlots(defenderSide)) {
      BoardCreature c = slot.creature();
      if (c == null || !c.hasSigil(sigil)) continue;
      if (slot.index() == targetLane) continue;
      if (canReachLane(match, c, targetLane)) {
        candidates.add(c);
      }
    }
    if (candidates.isEmpty()) return null;
    candidates.sort(
        Comparator.comparingInt(
            c -> Math.abs(c.slot().index() - targetLane)));
    return candidates.getFirst();
  }

  private static boolean canReachLane(
      InscriptionMatch match, BoardCreature creature, int targetLane) {
    BoardSlot slot = creature.slot();
    if (slot == null) return false;
    SlotOwner owner = slot.owner();
    return match.board().slot(owner, targetLane).isEmpty();
  }

  private static void applyTailOnHit(InscriptionMatch match, MatchSide attackerSide) {
    MatchSide defenderSide = attackerSide.opposite();
    List<BoardCreature> defenders = new ArrayList<>();
    for (BoardSlot slot : match.board().occupiedSlots(defenderSide)) {
      BoardCreature c = slot.creature();
      if (c != null && c.hasSigil(SigilId.TAIL_ON_HIT)) {
        defenders.add(c);
      }
    }
    for (BoardCreature defender : defenders) {
      if (!CombatTargeting.willCreatureBeAttacked(match, attackerSide, defender)) {
        continue;
      }
      splitTail(match, defender);
    }
  }

  private static void splitTail(InscriptionMatch match, BoardCreature defender) {
    BoardSlot from = defender.slot();
    if (from == null) return;
    int tailLane = from.index();
    int escapeLane = tailLane + 1;
    if (escapeLane >= BoardSlot.SLOT_COUNT) return;

    SlotOwner owner = from.owner();
    if (!match.board().slot(owner, escapeLane).isEmpty()) return;

    if (!BoardShift.moveToIndex(match, defender, escapeLane)) return;
    BoardTokens.spawnTokenOnSlot(match, defender.owner(), tailLane, CardId.TAIL.name());
  }
}
