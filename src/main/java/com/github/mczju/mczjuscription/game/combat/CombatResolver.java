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

    if (tryBeginPlayerBeamTargeting(attackerSide, slotIndex, onComplete)) {
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
    if (attacker.consumesSkipNextAttack()) {
      return List.of();
    }
    if (attacker.hasSigil(SigilId.ALL_STRIKE)) {
      return planAllStrike(attackerSide, attacker);
    }
    if (attacker.hasSigil(SigilId.TRI_STRIKE)) {
      return planLaneStrikes(attackerSide, attacker, slotIndex, -1, 0, 1);
    }
    if (attacker.hasSigil(SigilId.SPLIT_STRIKE)) {
      return planLaneStrikes(attackerSide, attacker, slotIndex, -1, 1);
    }
    if (attacker.hasSigil(SigilId.BEAM) && attackerSide != MatchSide.PLAYER) {
      SlotStrike beam = planBeamStrike(attackerSide, attacker);
      if (beam != null) {
        if (!attacker.hasSigil(SigilId.DOUBLE_STRIKE)) {
          return List.of(beam);
        }
        SlotStrike again = planBeamStrike(attackerSide, attacker);
        return again == null ? List.of(beam) : List.of(beam, again);
      }
    }
    SlotStrike single = planSingleStrike(attackerSide, slotIndex, attacker);
    if (single == null) return List.of();
    if (!attacker.hasSigil(SigilId.DOUBLE_STRIKE)) {
      return List.of(single);
    }
    SlotStrike again = planSingleStrike(attackerSide, slotIndex, attacker);
    if (again == null) return List.of(single);
    return List.of(single, again);
  }

  private boolean tryBeginPlayerBeamTargeting(
      MatchSide attackerSide, int slotIndex, Runnable onComplete) {
    if (attackerSide != MatchSide.PLAYER) return false;
    SlotOwner attackerOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
    BoardSlot attackerSlot = match.board().slot(attackerOwner, slotIndex);
    if (attackerSlot.isEmpty()) return false;
    BoardCreature attacker = attackerSlot.creature();
    if (!attacker.hasSigil(SigilId.BEAM) || attacker.consumesSkipNextAttack()) {
      return false;
    }
    Runnable advance =
        () ->
            CreatureAnimator.schedule(
                () -> resolveSlotAnimated(attackerSide, slotIndex + 1, onComplete),
                CreatureAnimator.PAUSE_TICKS);
    BeamTargeting.begin(match, this, attackerSide, slotIndex, attacker, 0, advance);
    return true;
  }

  /** 敌方 AI 射线：自动选目标（优先同列）。 */
  void resolveBeamWithoutTarget(
      MatchSide attackerSide, BoardCreature attacker, Runnable onComplete) {
    SlotStrike strike = planBeamStrike(attackerSide, attacker);
    if (strike == null) {
      onComplete.run();
      return;
    }
    playStrikesChain(List.of(strike), 0, onComplete);
  }

  void resolveBeamOnTarget(
      MatchSide attackerSide,
      BoardCreature attacker,
      BoardCreature defender,
      Runnable onComplete) {
    SlotStrike strike = beamStrikeOnTarget(attackerSide, attacker, defender);
    if (strike == null) {
      onComplete.run();
      return;
    }
    playStrikesChain(List.of(strike), 0, () -> {
      match.syncHud();
      onComplete.run();
    });
  }

  private SlotStrike beamStrikeOnTarget(
      MatchSide attackerSide, BoardCreature attacker, BoardCreature defender) {
    if (CombatModifiers.preventsAttack(defender)) return null;
    int atk = CombatModifiers.effectiveAttack(match.board(), attacker, defender);
    if (atk <= 0) return null;
    boolean instantKill = CombatModifiers.canDeathtouchKill(attacker, defender);
    return new CreatureStrike(match, attacker, defender, atk, instantKill);
  }

  /** 射线 AI：任选一格敌方造物作为目标（优先同列）。 */
  private SlotStrike planBeamStrike(MatchSide attackerSide, BoardCreature attacker) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    BoardSlot attackerSlot = attacker.slot();
    int preferred = attackerSlot == null ? 0 : attackerSlot.index();
    SlotStrike preferredStrike = null;
    for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
      BoardSlot defenderSlot = match.board().slot(defenderOwner, i);
      if (defenderSlot.isEmpty()) continue;
      SlotStrike strike = creatureStrike(attackerSide, attacker, defenderSlot.creature());
      if (strike == null) continue;
      if (i == preferred) return strike;
      if (preferredStrike == null) preferredStrike = strike;
    }
    if (preferredStrike != null) return preferredStrike;
    return directStrike(attackerSide, attacker);
  }

  /** 全向攻击：对面每一列各结算一次（空列 = 该列直伤）。 */
  private List<SlotStrike> planAllStrike(MatchSide attackerSide, BoardCreature attacker) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    int damage = CombatModifiers.effectiveAttack(match.board(), attacker, null);
    if (damage <= 0) return List.of();

    List<SlotStrike> strikes = new ArrayList<>();
    for (int lane = 0; lane < BoardSlot.SLOT_COUNT; lane++) {
      strikes.addAll(planStrikeAtDefenderLane(attackerSide, attacker, defenderOwner, lane, damage));
    }
    return strikes;
  }

  private List<SlotStrike> planLaneStrikes(
      MatchSide attackerSide, BoardCreature attacker, int centerIndex, int... deltas) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    int damage = CombatModifiers.effectiveAttack(match.board(), attacker, null);
    if (damage <= 0) return List.of();

    List<SlotStrike> strikes = new ArrayList<>();
    for (int delta : deltas) {
      int idx = centerIndex + delta;
      if (idx < 0 || idx >= BoardSlot.SLOT_COUNT) continue;
      strikes.addAll(planStrikeAtDefenderLane(attackerSide, attacker, defenderOwner, idx, damage));
    }
    return strikes;
  }

  /** 对敌方指定列：有造物则对战，空列则直伤。 */
  private List<SlotStrike> planStrikeAtDefenderLane(
      MatchSide attackerSide,
      BoardCreature attacker,
      SlotOwner defenderOwner,
      int defenderLane,
      int directDamage) {
    List<SlotStrike> strikes = new ArrayList<>();
    BoardSlot defenderSlot = match.board().slot(defenderOwner, defenderLane);
    if (defenderSlot.isEmpty()) {
      SlotStrike direct = directStrikeLane(attackerSide, attacker, defenderLane, directDamage);
      if (direct != null) strikes.add(direct);
      return strikes;
    }
    SlotStrike melee = creatureStrike(attackerSide, attacker, defenderSlot.creature());
    if (melee != null) strikes.add(melee);
    return strikes;
  }

  private SlotStrike planSingleStrike(
      MatchSide attackerSide, int slotIndex, BoardCreature attacker) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    BoardSlot defenderSlot = match.board().slot(defenderOwner, slotIndex);
    BoardCreature defender = defenderSlot.isEmpty() ? null : defenderSlot.creature();
    boolean submerged =
        defender != null
            && CombatModifiers.isSubmerged(match.board(), defender, attackerSide);

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
    boolean submerged = CombatModifiers.isSubmerged(match.board(), defender, attackerSide);
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
    int damage = CombatModifiers.effectiveAttack(match.board(), attacker, null);
    if (damage <= 0) return null;
    int lane = attacker.slot() == null ? 0 : attacker.slot().index();
    return directStrikeLane(attackerSide, attacker, lane, damage);
  }

  private SlotStrike directStrikeLane(
      MatchSide attackerSide, BoardCreature attacker, int defenderLane, int damage) {
    if (damage <= 0) return null;
    return new DirectStrike(match, attacker, attackerSide.opposite(), damage, defenderLane);
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
      if (defender.absorbFirstHitWithShield()) {
        return;
      }
      int dmg = CombatModifiers.capIncomingDamage(defender, atk);
      defender.damage(dmg);
      SigilRegistry.fire(
          SigilTrigger.ON_COMBAT_ATTACK,
          new SigilContext(match, SigilTrigger.ON_COMBAT_ATTACK, attacker, defender, atk));
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
    private final int defenderLane;

    private DirectStrike(
        InscriptionMatch match,
        BoardCreature attacker,
        MatchSide victimSide,
        int damage,
        int defenderLane) {
      this.match = match;
      this.attacker = attacker;
      this.victimSide = victimSide;
      this.damage = damage;
      this.defenderLane = defenderLane;
    }

    @Override
    public void play(Runnable onFinished) {
      Location home = standOf(match, attacker);
      Location target = directAttackLaneTarget(match, attacker, defenderLane);
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
      SigilRegistry.fire(
          SigilTrigger.ON_COMBAT_ATTACK,
          new SigilContext(match, SigilTrigger.ON_COMBAT_ATTACK, attacker, null, damage));
      match.checkRoundEnd();
    }
  }

  private static Location standOf(InscriptionMatch match, BoardCreature creature) {
    Location slot = BoardVfx.locationOf(match, creature);
    return CreatureAnimator.slotStand(slot);
  }

  private static Location directAttackLaneTarget(
      InscriptionMatch match, BoardCreature attacker, int defenderLane) {
    if (match.arena() != null) {
      SlotOwner laneRow = attacker.owner() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
      Location inFront = match.arena().slotLocation(laneRow, defenderLane);
      if (inFront != null) {
        return CreatureAnimator.slotStand(inFront);
      }
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
