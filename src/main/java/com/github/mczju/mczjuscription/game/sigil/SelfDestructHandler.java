package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/** 【自爆】：死亡时对相邻格与面前格造成 10 点伤害。 */
public final class SelfDestructHandler {

  private static final int DAMAGE = 10;

  private SelfDestructHandler() {}

  public static void explode(InscriptionMatch match, BoardCreature source) {
    if (!source.hasSigil(SigilId.SELF_DESTRUCT)) return;
    if (HissAura.suppressesSelfDestruct(match, source.owner())) return;
    BoardSlot slot = source.slot();
    if (slot == null) return;

    int lane = slot.index();
    SlotOwner owner = slot.owner();
    damageLane(match, owner, lane, source.owner());
    for (int delta : new int[] {-1, 1}) {
      int idx = lane + delta;
      if (idx < 0 || idx >= BoardSlot.SLOT_COUNT) continue;
      damageLane(match, owner, idx, source.owner());
    }
  }

  private static void damageLane(
      InscriptionMatch match, SlotOwner rowOwner, int lane, MatchSide bomberSide) {
    BoardSlot s = match.board().slot(rowOwner, lane);
    if (!s.isEmpty() && s.creature() != null) {
      BoardCreature c = s.creature();
      if (c.absorbFirstHitWithShield()) return;
      int dmg = com.github.mczju.mczjuscription.game.combat.CombatModifiers.capIncomingDamage(c, DAMAGE);
      c.damage(dmg);
      if (c.isDead()) {
        match.killCreature(c, bomberSide, false);
      }
      return;
    }
    MatchSide victimSide = rowOwner == SlotOwner.PLAYER ? MatchSide.PLAYER : MatchSide.ENEMY;
    if (victimSide == MatchSide.PLAYER) {
      match.scales().damagePlayer(DAMAGE);
    } else {
      match.scales().damageEnemy(DAMAGE);
    }
    match.checkRoundEnd();
  }
}
