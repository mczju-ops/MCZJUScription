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
import com.github.mczju.mczjuscription.vfx.CreatureAttackVfx;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.bukkit.Location;

/**
 * 战斗结算。开战前快照己方造物，按快照顺序（原槽位 0→3）依次攻击；
 * 攻击中位移（如【蓄风】）不会触发额外攻击轮次。
 */
public final class CombatResolver {

  private final InscriptionMatch match;

  /** 开战瞬间的造物及其原列，用于整轮战斗的目标列与攻击顺序。 */
  private record CombatAttacker(BoardCreature creature, int laneAtCombatStart) {}

  public CombatResolver(InscriptionMatch match) {
    this.match = match;
  }

  public void resolveSideCombatAnimated(MatchSide attackerSide, Runnable onComplete) {
    List<CombatAttacker> attackers = snapshotAttackers(attackerSide);
    resolveAttackerAnimated(attackerSide, attackers, 0, onComplete);
  }

  private List<CombatAttacker> snapshotAttackers(MatchSide attackerSide) {
    SlotOwner owner = attackerSide == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
    List<CombatAttacker> attackers = new ArrayList<>();
    for (int lane = 0; lane < BoardSlot.SLOT_COUNT; lane++) {
      BoardSlot slot = match.board().slot(owner, lane);
      if (!slot.isEmpty() && slot.creature() != null) {
        attackers.add(new CombatAttacker(slot.creature(), lane));
      }
    }
    return attackers;
  }

  private void resolveAttackerAnimated(
      MatchSide attackerSide,
      List<CombatAttacker> attackers,
      int attackerIndex,
      Runnable onComplete) {
    if (attackerIndex >= attackers.size() || match.shouldStopCombatSequence()) {
      if (onComplete != null) onComplete.run();
      return;
    }

    CombatAttacker entry = attackers.get(attackerIndex);
    BoardCreature attacker = entry.creature();
    if (!canStillAttack(attacker)) {
      CreatureAnimator.schedule(
          () -> resolveAttackerAnimated(attackerSide, attackers, attackerIndex + 1, onComplete),
          4L);
      return;
    }

    int lane = entry.laneAtCombatStart();
    if (tryBeginPlayerBeamTargeting(
        attackerSide, lane, attacker, attackerIndex, attackers, onComplete)) {
      return;
    }

    List<SlotStrike> strikes = planStrikes(attackerSide, lane, attacker);
    if (strikes.isEmpty()) {
      CreatureAnimator.schedule(
          () -> resolveAttackerAnimated(attackerSide, attackers, attackerIndex + 1, onComplete),
          4L);
      return;
    }

    playStrikesChain(
        strikes,
        0,
        () -> {
          match.syncHud();
          if (match.shouldStopCombatSequence()) {
            if (onComplete != null) onComplete.run();
            return;
          }
          CreatureAnimator.schedule(
              () ->
                  resolveAttackerAnimated(attackerSide, attackers, attackerIndex + 1, onComplete),
              CreatureAnimator.PAUSE_TICKS);
        });
  }

  private static boolean canStillAttack(BoardCreature attacker) {
    return !attacker.isDead()
        && attacker.slot() != null
        && attacker.slot().creature() == attacker;
  }

  private void playStrikesChain(List<SlotStrike> strikes, int index, Runnable onAllDone) {
    if (index >= strikes.size() || match.shouldStopCombatSequence()) {
      onAllDone.run();
      return;
    }
    SlotStrike strike = strikes.get(index);
    strike.play(
        () -> {
          if (match.shouldStopCombatSequence()) {
            onAllDone.run();
            return;
          }
          strike.apply();
          if (match.shouldStopCombatSequence()) {
            onAllDone.run();
            return;
          }
          CreatureAnimator.schedule(
              () -> playStrikesChain(strikes, index + 1, onAllDone), CreatureAnimator.PAUSE_TICKS);
        });
  }

  private List<SlotStrike> planStrikes(
      MatchSide attackerSide, int laneAtCombatStart, BoardCreature attacker) {
    if (attacker.consumesSkipNextAttack()) {
      return List.of();
    }
    if (attacker.hasSigil(SigilId.ALL_STRIKE)) {
      return planAllStrike(attackerSide, attacker);
    }
    if (attacker.hasSigil(SigilId.TRI_STRIKE)) {
      return planLaneStrikes(attackerSide, attacker, laneAtCombatStart, -1, 0, 1);
    }
    if (attacker.hasSigil(SigilId.SPLIT_STRIKE)) {
      return planLaneStrikes(attackerSide, attacker, laneAtCombatStart, -1, 1);
    }
    if (attacker.hasSigil(SigilId.BEAM) && attackerSide != MatchSide.PLAYER) {
      SlotStrike beam = planBeamStrike(attackerSide, attacker, laneAtCombatStart);
      if (beam != null) {
        if (!attacker.hasSigil(SigilId.DOUBLE_STRIKE)) {
          return List.of(beam);
        }
        return List.of(beam, deferredBeamStrike(attackerSide, attacker, laneAtCombatStart));
      }
    }
    SlotStrike single = planSingleStrike(attackerSide, laneAtCombatStart, attacker);
    if (single == null) return List.of();
    if (!attacker.hasSigil(SigilId.DOUBLE_STRIKE)) {
      return List.of(single);
    }
    return List.of(single, deferredSingleStrike(attackerSide, laneAtCombatStart, attacker));
  }

  /** 连击第二击：在第一击结算后再规划（对面已死则走空位直伤）。 */
  private SlotStrike deferredSingleStrike(
      MatchSide attackerSide, int laneAtCombatStart, BoardCreature attacker) {
    return deferredStrike(() -> planSingleStrike(attackerSide, laneAtCombatStart, attacker));
  }

  private SlotStrike deferredBeamStrike(
      MatchSide attackerSide, BoardCreature attacker, int laneAtCombatStart) {
    return deferredStrike(() -> planBeamStrike(attackerSide, attacker, laneAtCombatStart));
  }

  private boolean tryBeginPlayerBeamTargeting(
      MatchSide attackerSide,
      int laneAtCombatStart,
      BoardCreature attacker,
      int attackerIndex,
      List<CombatAttacker> attackers,
      Runnable onComplete) {
    if (attackerSide != MatchSide.PLAYER) return false;
    if (!attacker.hasSigil(SigilId.BEAM) || attacker.consumesSkipNextAttack()) {
      return false;
    }
    Runnable advance =
        () ->
            CreatureAnimator.schedule(
                () ->
                    resolveAttackerAnimated(
                        attackerSide, attackers, attackerIndex + 1, onComplete),
                CreatureAnimator.PAUSE_TICKS);
    BeamTargeting.begin(match, this, attackerSide, laneAtCombatStart, attacker, 0, advance);
    return true;
  }

  /** 敌方 AI 射线：自动选目标（优先同列）。 */
  void resolveBeamWithoutTarget(
      MatchSide attackerSide, BoardCreature attacker, Runnable onComplete) {
    if (match.shouldStopCombatSequence()) {
      onComplete.run();
      return;
    }
    int lane = attacker.slot() == null ? 0 : attacker.slot().index();
    SlotStrike strike = planBeamStrike(attackerSide, attacker, lane);
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
    if (match.shouldStopCombatSequence()) {
      onComplete.run();
      return;
    }
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

  /** 射线确认空列或直伤列。 */
  void resolveBeamOnLane(
      MatchSide attackerSide, BoardCreature attacker, int defenderLane, Runnable onComplete) {
    if (match.shouldStopCombatSequence()) {
      onComplete.run();
      return;
    }
    int damage = CombatModifiers.effectiveAttack(match.board(), attacker, null);
    SlotStrike strike = directStrikeLane(attackerSide, attacker, defenderLane, damage);
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
  private SlotStrike planBeamStrike(
      MatchSide attackerSide, BoardCreature attacker, int preferredLane) {
    SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    SlotStrike preferredStrike = null;
    for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
      BoardSlot defenderSlot = match.board().slot(defenderOwner, i);
      if (defenderSlot.isEmpty()) continue;
      SlotStrike strike = creatureStrike(attackerSide, attacker, defenderSlot.creature());
      if (strike == null) continue;
      if (i == preferredLane) return strike;
      if (preferredStrike == null) preferredStrike = strike;
    }
    if (preferredStrike != null) return preferredStrike;
    return directStrikeLane(
        attackerSide,
        attacker,
        preferredLane,
        CombatModifiers.effectiveAttack(match.board(), attacker, null));
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
      int damage = CombatModifiers.effectiveAttack(match.board(), attacker, null);
      return directStrikeLane(attackerSide, attacker, slotIndex, damage);
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

  private SlotStrike deferredStrike(Supplier<SlotStrike> planner) {
    return new DeferredStrike(planner);
  }

  private SlotStrike directStrikeLane(
      MatchSide attackerSide, BoardCreature attacker, int defenderLane, int damage) {
    if (damage <= 0) return null;
    return new DirectStrike(match, attacker, attackerSide.opposite(), damage, defenderLane);
  }

  private sealed interface SlotStrike permits CreatureStrike, DirectStrike, DeferredStrike {
    void play(Runnable onFinished);

    void apply();
  }

  /** 延迟到播放时再规划（用于连击第二击等需读取最新场面状态的情况）。 */
  private static final class DeferredStrike implements SlotStrike {
    private final Supplier<SlotStrike> planner;
    private SlotStrike delegate;

    private DeferredStrike(Supplier<SlotStrike> planner) {
      this.planner = planner;
    }

    private SlotStrike resolve() {
      if (delegate == null) {
        delegate = planner.get();
      }
      return delegate;
    }

    @Override
    public void play(Runnable onFinished) {
      SlotStrike strike = resolve();
      if (strike == null) {
        onFinished.run();
        return;
      }
      strike.play(onFinished);
    }

    @Override
    public void apply() {
      SlotStrike strike = resolve();
      if (strike != null) {
        strike.apply();
      }
    }
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
          () ->
              playAttackVfx(match, attacker, home, target, defender, atk, instantKill),
          onFinished);
    }

    @Override
    public void apply() {
      if (defender.isDead()
          || defender.slot() == null
          || defender.slot().isEmpty()
          || defender.slot().creature() != defender) {
        return;
      }
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
          () ->
              CreatureAttackVfx.play(
                  attacker,
                  home,
                  target,
                  () -> BoardVfx.playDirectDamage(match, victimSide, defenderLane, damage)),
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
    return CreatureAnimator.slotStand(creature, slot);
  }

  private static Location directAttackLaneTarget(
      InscriptionMatch match, BoardCreature attacker, int defenderLane) {
    MatchSide victimSide = attacker.owner().opposite();
    Location inFront = BoardVfx.slotLocation(match, victimSide, defenderLane);
    if (inFront != null) {
      return CreatureAnimator.slotStand(attacker, inFront);
    }
    return standOf(match, attacker);
  }

  private static void playAttackVfx(
      InscriptionMatch match,
      BoardCreature attacker,
      Location from,
      Location to,
      BoardCreature defender,
      int damage,
      boolean instantKill) {
    CreatureAttackVfx.play(
        attacker,
        from,
        to,
        () -> {
          Location loc = BoardVfx.locationOf(match, defender);
          if (loc != null) {
            BoardVfx.playAttackAt(loc, damage, instantKill);
          }
        });
  }
}
