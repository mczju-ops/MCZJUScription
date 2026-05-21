package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BoardShift;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 战前印记：守卫者挡空列直击。 */
public final class PreCombatResolver {

  private PreCombatResolver() {}

  public static void resolve(InscriptionMatch match, MatchSide attackerSide) {
    blockEmptyLanes(match, attackerSide, SigilId.GUARD_DOG);
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
    candidates.sort(Comparator.comparingInt(c -> Math.abs(c.slot().index() - targetLane)));
    return candidates.getFirst();
  }

  private static boolean canReachLane(
      InscriptionMatch match, BoardCreature creature, int targetLane) {
    BoardSlot slot = creature.slot();
    if (slot == null) return false;
    SlotOwner owner = slot.owner();
    return match.board().slot(owner, targetLane).isEmpty();
  }
}
