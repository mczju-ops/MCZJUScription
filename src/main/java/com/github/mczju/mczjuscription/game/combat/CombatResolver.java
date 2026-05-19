package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.entity.CreatureAnimator;
import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilContext;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilRegistry;
import com.github.mczju.mczjuscription.game.sigil.SigilTrigger;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;

/**
 * 战斗结算。按槽位 0→3 顺序播放攻击动画；支持兵分两路 / 三路 / 全向打击。
 */
public final class CombatResolver {

  private final InscriptionMatch match;

  public CombatResolver(InscriptionMatch match) {
    this.match = match;
  }

  public void resolveSideCombatAnimated(MatchSide attackerSide, Runnable onComplete) {
    resolveSlotAnimated(attackerSide, 0, onComplete);
  }

  private void resolveSlotAnimated(MatchSide attackerSide, int slotIndex, Runnable onComplete) {
    if (slotIndex >= BoardSlot.SLOT_COUNT || match.isMatchOver()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    List<SlotStrike> strikes = planStrikes(attackerSide, slotIndex);
    if (strikes.isEmpty()) {
      CreatureAnimator.schedule(
          () -> resolveSlotAnimated(attackerSide, slotIndex + 1, onComplete), 4L);
      return;
    }

    playStrikesChain(strikes, 0, () -> {
      match.syncHud();
      if (match.isMatchOver()) {
        if (onComplete != null) onComplete.run();
        return;
      }
      CreatureAnimator.schedule(
          () -> resolveSlotAnimated(attackerSide, slotIndex + 1, onComplete),
          CreatureAnimator.PAUSE_TICKS);
    });
  }

  private void playStrikesChain(List<SlotStrike> strikes, int index, Runnable onAllDone) {
    if (index >= strikes.size() || match.isMatchOver()) {
      onAllDone.run();
      return;
    }
    SlotStrike strike = strikes.get(index);
    strike.play(
        () -> {
          strike.apply();
          if (match.isMatchOver()) {
            onAllDone.run();
            return;
          }
          CreatureAnimator.schedule(
              () -> playStrikesChain(strikes, index + 1, onAllDone), CreatureAnimator.PAUSE_TICKS);
        });
  }

  private List<SlotStrike> planStrikes(MatchSide attackerSide, int slotIndex) {
    SlotOwner attackerOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
    BattleBoard board = match.board();
    BoardSlot attackerSlot = board.slot(attackerOwner, slotIndex);
    if (attackerSlot.isEmpty()) return List.of();

    BoardCreature attacker = attackerSlot.creature();
    if (attacker.hasSigil(SigilId.ALL_STRIKE)) {
      return planAllStrike(attackerSide, attacker);
    }
    if (attacker.hasSigil(SigilId.TRI_STRIKE)) {
      return planLaneStrikes(attackerSide, attacker, slotIndex, -1, 0, 1);
    }
    if (attacker.hasSigil(SigilId.SPLIT_STRIKE)) {
      return planLaneStrikes(attackerSide, attacker, slotIndex, -1, 1);
    }
    SlotStrike single = planSingleStrike(attackerSide, slotIndex, attacker);
    return single == null ? List.of() : List.of(single);
  }

  private List<SlotStrike> planAllStrike(MatchSide attackerSide, BoardCreature attacker) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    List<SlotStrike> strikes = new ArrayList<>();
    for (BoardSlot slot : match.board().row(defenderOwner)) {
      if (slot.isEmpty()) continue;
      SlotStrike strike = creatureStrike(attackerSide, attacker, slot.creature());
      if (strike != null) strikes.add(strike);
    }
    if (strikes.isEmpty()) {
      SlotStrike direct = directStrike(attackerSide, attacker);
      if (direct != null) strikes.add(direct);
    }
    return strikes;
  }

  private List<SlotStrike> planLaneStrikes(
      MatchSide attackerSide, BoardCreature attacker, int centerIndex, int... deltas) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    List<SlotStrike> strikes = new ArrayList<>();
    for (int delta : deltas) {
      int idx = centerIndex + delta;
      if (idx < 0 || idx >= BoardSlot.SLOT_COUNT) continue;
      BoardSlot defenderSlot = match.board().slot(defenderOwner, idx);
      if (defenderSlot.isEmpty()) continue;
      SlotStrike strike = creatureStrike(attackerSide, attacker, defenderSlot.creature());
      if (strike != null) strikes.add(strike);
    }
    return strikes;
  }

  private SlotStrike planSingleStrike(
      MatchSide attackerSide, int slotIndex, BoardCreature attacker) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    BoardSlot defenderSlot = match.board().slot(defenderOwner, slotIndex);
    BoardCreature defender = defenderSlot.isEmpty() ? null : defenderSlot.creature();
    boolean submerged =
        defender != null && CombatModifiers.isSubmerged(defender, attackerSide);

    if (defender != null && CombatModifiers.preventsAttack(defender)) {
      return null;
    }

    if (CombatModifiers.mustFightDefender(attacker, defender, submerged)) {
      int atk = CombatModifiers.effectiveAttack(match.board(), attacker, defender);
      if (atk <= 0) return null;
      return creatureStrike(attackerSide, attacker, defender, atk);
    }

    if (defender == null || submerged || attacker.hasSigil(SigilId.AIR_STRIKE)) {
      return directStrike(attackerSide, attacker);
    }
    return null;
  }

  private SlotStrike creatureStrike(
      MatchSide attackerSide, BoardCreature attacker, BoardCreature defender) {
    boolean submerged = CombatModifiers.isSubmerged(defender, attackerSide);
    if (CombatModifiers.preventsAttack(defender)) return null;
    if (!CombatModifiers.mustFightDefender(attacker, defender, submerged)) return null;
    int atk = CombatModifiers.effectiveAttack(match.board(), attacker, defender);
    if (atk <= 0) return null;
    return creatureStrike(attackerSide, attacker, defender, atk);
  }

  private SlotStrike creatureStrike(
      MatchSide attackerSide,
      BoardCreature attacker,
      BoardCreature defender,
      int atk) {
    boolean instantKill = CombatModifiers.canDeathtouchKill(attacker, defender);
    return new CreatureStrike(match, attacker, defender, atk, instantKill);
  }

  private SlotStrike directStrike(MatchSide attackerSide, BoardCreature attacker) {
    int damage =
        attacker.currentAttack() + CombatModifiers.leaderBonus(match.board(), attacker);
    if (damage <= 0) return null;
    return new DirectStrike(match, attacker, attackerSide.opposite(), damage);
  }

  private sealed interface SlotStrike permits CreatureStrike, DirectStrike {
    void play(Runnable onFinished);

    void apply();
  }

  private static final class CreatureStrike implements SlotStrike {
    private final InscriptionMatch match;
    private final BoardCreature attacker;
    private final BoardCreature defender;
    private final int atk;
    private final boolean instantKill;

    private CreatureStrike(
        InscriptionMatch match,
        BoardCreature attacker,
        BoardCreature defender,
        int atk,
        boolean instantKill) {
      this.match = match;
      this.attacker = attacker;
      this.defender = defender;
      this.atk = atk;
      this.instantKill = instantKill;
    }

    @Override
    public void play(Runnable onFinished) {
      Location home = standOf(match, attacker);
      Location target = standOf(match, defender);
      CreatureAnimator.playAttackSequence(
          attacker,
          home,
          target,
          () -> playAttackVfx(match, defender, atk, instantKill),
          onFinished);
    }

    @Override
    public void apply() {
      if (instantKill) {
        match.killCreature(defender, attacker.owner(), false);
        return;
      }
      defender.damage(atk);
      SigilRegistry.fire(
          SigilTrigger.ON_ATTACKED,
          new SigilContext(match, SigilTrigger.ON_ATTACKED, defender, attacker, atk));
      if (defender.isDead()) {
        match.killCreature(defender, attacker.owner(), false);
      }
    }
  }

  private static final class DirectStrike implements SlotStrike {
    private final InscriptionMatch match;
    private final BoardCreature attacker;
    private final MatchSide victimSide;
    private final int damage;

    private DirectStrike(
        InscriptionMatch match, BoardCreature attacker, MatchSide victimSide, int damage) {
      this.match = match;
      this.attacker = attacker;
      this.victimSide = victimSide;
      this.damage = damage;
    }

    @Override
    public void play(Runnable onFinished) {
      Location home = standOf(match, attacker);
      Location target = directAttackLaneTarget(match, attacker);
      CreatureAnimator.playAttackSequence(
          attacker,
          home,
          target,
          () -> BoardVfx.playDirectDamage(match, victimSide, damage),
          onFinished);
    }

    @Override
    public void apply() {
      if (victimSide == MatchSide.PLAYER) {
        match.scales().damagePlayer(damage);
      } else {
        match.scales().damageEnemy(damage);
      }
      match.checkRoundEnd();
    }
  }

  private static Location standOf(InscriptionMatch match, BoardCreature creature) {
    Location slot = BoardVfx.locationOf(match, creature);
    return CreatureAnimator.slotStand(slot);
  }

  private static Location directAttackLaneTarget(InscriptionMatch match, BoardCreature attacker) {
    if (match.arena() == null || attacker.slot() == null) {
      return standOf(match, attacker);
    }
    SlotOwner laneRow = attacker.owner() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    Location inFront = match.arena().slotLocation(laneRow, attacker.slot().index());
    if (inFront != null) {
      return CreatureAnimator.slotStand(inFront);
    }
    return standOf(match, attacker);
  }

  private static void playAttackVfx(
      InscriptionMatch match, BoardCreature defender, int damage, boolean instantKill) {
    Location loc = BoardVfx.locationOf(match, defender);
    if (loc != null) {
      BoardVfx.playAttackAt(loc, damage, instantKill);
    }
  }
}
