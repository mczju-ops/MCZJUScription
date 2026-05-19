package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilId;

/** 战斗中的印记修正：力量、空袭拦截、潜水、死触免疫等。 */
public final class CombatModifiers {

  private CombatModifiers() {}

  public static boolean isSubmerged(BoardCreature defender, MatchSide attackerSide) {
    return defender.hasSigil(SigilId.WATER_STRIKE) && defender.owner() != attackerSide;
  }

  /** 高跳：带空袭的攻击者必须与本列防御者对战，不能直击。 */
  public static boolean mustFightDefender(
      BoardCreature attacker, BoardCreature defender, boolean submerged) {
    if (defender == null || submerged) return false;
    if (!attacker.hasSigil(SigilId.AIR_STRIKE)) return true;
    return defender.hasSigil(SigilId.HIGH_JUMP);
  }

  public static boolean preventsAttack(BoardCreature defender) {
    return defender.hasSigil(SigilId.PREVENT_ATTACK);
  }

  public static boolean canDeathtouchKill(BoardCreature attacker, BoardCreature defender) {
    if (!attacker.hasSigil(SigilId.TOUCH_OF_DEATH)) return false;
    return !(defender.hasSigil(SigilId.ROCK_BODY) && defender.owner() != attacker.owner());
  }

  public static int effectiveAttack(
      BattleBoard board, BoardCreature attacker, BoardCreature defender) {
    int atk = attacker.currentAttack() + leaderBonus(board, attacker);
    if (defender != null
        && defender.hasSigil(SigilId.STINKY)
        && !attacker.hasSigil(SigilId.ROCK_BODY)
        && attacker.owner() != defender.owner()) {
      atk = Math.max(0, atk - 1);
    }
    return atk;
  }

  /** 领袖力量：左右相邻友方 +1 力量。 */
  public static int leaderBonus(BattleBoard board, BoardCreature creature) {
    BoardSlot slot = creature.slot();
    if (slot == null) return 0;
    SlotOwner owner =
        creature.owner() == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
    BoardSlot[] row = board.row(owner);
    int idx = slot.index();
    int bonus = 0;
    for (int delta : new int[] {-1, 1}) {
      int i = idx + delta;
      if (i < 0 || i >= BoardSlot.SLOT_COUNT) continue;
      BoardCreature neighbor = row[i].creature();
      if (neighbor != null && neighbor.hasSigil(SigilId.LEADER_POWER)) {
        bonus++;
      }
    }
    return bonus;
  }
}
