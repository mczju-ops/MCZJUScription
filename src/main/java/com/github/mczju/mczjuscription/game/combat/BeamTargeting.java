package com.github.mczju.mczjuscription.game.combat;



import com.github.mczju.mczjuscription.arena.ArenaPedalTarget;

import com.github.mczju.mczjuscription.arena.ArenaSlotSounds;

import com.github.mczju.mczjuscription.arena.BattleArena;

import com.github.mczju.mczjuscription.entity.CreatureEntityService;

import com.github.mczju.mczjuscription.game.board.BoardSlot;

import com.github.mczju.mczjuscription.game.board.SlotOwner;

import com.github.mczju.mczjuscription.game.card.BoardCreature;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

import com.github.mczju.mczjuscription.game.match.MatchSide;

import com.github.mczju.mczjuscription.game.sigil.SigilId;

import java.util.ArrayList;

import java.util.List;

import java.util.UUID;



/** 【射线】：玩家用献祭之剑左键射线踏板选定并确认攻击目标（含空槽直伤）。 */

public final class BeamTargeting {



  private static final long FLASH_TICKS = 8L;



  private BeamTargeting() {}



  public static void begin(
      InscriptionMatch match,
      CombatResolver resolver,
      MatchSide attackerSide,
      int slotIndex,
      BoardCreature attacker,
      int strikeRound,
      Runnable onSlotComplete) {
    openBeamSession(
        match,
        resolver,
        attackerSide,
        slotIndex,
        attacker,
        strikeRound,
        onSlotComplete,
        null,
        -1);
  }

  /** 【兵分三路 + 射线】：按左→中→右依次进入射线选目标；带【追击】时每列连选连打两次。 */
  public static void beginTriBeam(
      InscriptionMatch match,
      CombatResolver resolver,
      MatchSide attackerSide,
      int slotIndex,
      BoardCreature attacker,
      int[] triDefenderLanes,
      int triPhaseIndex,
      int strikeRound,
      Runnable onSlotComplete) {
    if (triDefenderLanes == null
        || triPhaseIndex < 0
        || triPhaseIndex >= triDefenderLanes.length) {
      onSlotComplete.run();
      return;
    }
    openBeamSession(
        match,
        resolver,
        attackerSide,
        slotIndex,
        attacker,
        strikeRound,
        onSlotComplete,
        triDefenderLanes,
        triPhaseIndex);
  }

  private static void openBeamSession(
      InscriptionMatch match,
      CombatResolver resolver,
      MatchSide attackerSide,
      int slotIndex,
      BoardCreature attacker,
      int strikeRound,
      Runnable onSlotComplete,
      int[] triDefenderLanes,
      int triPhaseIndex) {
    cancel(match);

    List<BoardCreature> targets = listSelectableTargets(match, attackerSide, attacker);
    if (targets.isEmpty()
        && CombatModifiers.effectiveAttack(match.board(), attacker, null) <= 0) {
      if (triDefenderLanes != null && triPhaseIndex >= 0) {
        int next = triPhaseIndex + 1;
        if (next < triDefenderLanes.length) {
          beginTriBeam(
              match,
              resolver,
              attackerSide,
              slotIndex,
              attacker,
              triDefenderLanes,
              next,
              strikeRound,
              onSlotComplete);
        } else {
          onSlotComplete.run();
        }
        return;
      }
      resolver.resolveBeamWithoutTarget(attackerSide, attacker, onSlotComplete);
      return;
    }

    BeamCombatSession session =
        triDefenderLanes != null
            ? new BeamCombatSession(
                resolver,
                attackerSide,
                slotIndex,
                attacker,
                strikeRound,
                onSlotComplete,
                triDefenderLanes,
                triPhaseIndex)
            : new BeamCombatSession(
                resolver, attackerSide, slotIndex, attacker, strikeRound, onSlotComplete);
    match.setBeamSession(session);

    match.feedback()
        .announceInfo(
            "<gold>【%s】<white>：请用献祭之剑指定攻击列"
                .formatted(attacker.displayName()));
    if (session.isTriBeam()) {
      int col = triDefenderLanes[triPhaseIndex];
      String phase = triPhaseLabel(triPhaseIndex, triDefenderLanes.length);
      String suffix = strikeRound > 0 ? "（追击）" : "";
      match.feedback()
          .actionBarInfo(
              "<yellow>【兵分三路·射线】%s · 第 %d 列%s：左键选目标，再次确认"
                  .formatted(phase, col + 1, suffix));
    } else {
      match.feedback()
          .actionBarInfo("<yellow>左键敌方槽位选定；再次左键同一槽位确认（空槽=直伤）");
    }
  }



  /**

   * @return true 已消费本次点击（射线选目标）

   */

  public static boolean handleSacrificeClick(

      InscriptionMatch match, MatchSide playerSide, BoardCreature clicked) {

    BeamCombatSession session = match.beamSession();

    if (session == null || session.attackerSide() != playerSide) {

      return false;

    }

    if (clicked.owner() == playerSide) {

      return false;

    }

    if (clicked.owner() != playerSide.opposite()) {

      return false;

    }



    BoardCreature attacker = match.findCreatureByInstanceId(session.attackerId());

    if (attacker == null || attacker.isDead()) {

      cancel(match);

      session.onSlotComplete().run();

      return true;

    }



    if (!isLaneStrikeable(match, session.attackerSide(), attacker, clicked)) {

      match.feedback().actionBarWarn("<red>无法攻击该列（震慑或攻击力为 0）");
      beamSound(match, session.attackerSide(), ArenaSlotSounds.Kind.REJECT);

      return true;

    }



    int lane = clicked.slot() == null ? 0 : clicked.slot().index();

    return handleLaneSelection(match, session, attacker, lane, clicked);

  }



  /** 踏板版射线选目标。 */

  public static boolean handlePedalClick(

      InscriptionMatch match, MatchSide playerSide, ArenaPedalTarget target) {

    BeamCombatSession session = match.beamSession();

    if (session == null || session.attackerSide() != playerSide) {

      return false;

    }

    if (!(target instanceof ArenaPedalTarget.Battle battle)) {

      return false;

    }



    SlotOwner defenderOwner =

        playerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;

    if (battle.owner() != defenderOwner) {

      match.feedback().actionBarWarn("<red>请选择敌方站场槽位");
      beamSound(match, playerSide, ArenaSlotSounds.Kind.REJECT);

      return true;

    }



    BoardCreature attacker = match.findCreatureByInstanceId(session.attackerId());

    if (attacker == null || attacker.isDead()) {

      cancel(match);

      session.onSlotComplete().run();

      return true;

    }



    BoardSlot slot = match.board().slot(battle.owner(), battle.index());

    BoardCreature defender = slot.isEmpty() ? null : slot.creature();

    if (!isLaneStrikeable(match, session.attackerSide(), attacker, defender)) {

      match.feedback().actionBarWarn("<red>无法攻击该列（震慑或攻击力为 0）");
      beamSound(match, session.attackerSide(), ArenaSlotSounds.Kind.REJECT);

      return true;

    }



    return handleLaneSelection(match, session, attacker, battle.index(), defender);

  }



  public static void cancel(InscriptionMatch match) {

    BeamCombatSession session = match.beamSession();

    if (session == null) {

      return;

    }

    clearPendingGlow(match, session);

    match.setBeamSession(null);

  }



  private static boolean handleLaneSelection(

      InscriptionMatch match,

      BeamCombatSession session,

      BoardCreature attacker,

      int lane,

      BoardCreature defender) {

    Integer pendingLane = session.pendingLaneIndex();

    UUID pendingId = session.pendingTargetId();



    if (pendingLane != null && pendingLane == lane) {

      confirmLaneStrike(match, session, attacker, lane, defender);

      return true;

    }

    if (pendingId != null && defender != null && pendingId.equals(defender.instanceId())) {

      confirmLaneStrike(match, session, attacker, lane, defender);

      return true;

    }



    clearPendingGlow(match, session);

    session.setPendingLaneIndex(lane);

    if (defender != null) {

      session.setPendingTargetId(defender.instanceId());

      CreatureEntityService.setCombatGlow(defender, true);

    } else {

      session.setPendingTargetId(null);

    }



    BattleArena arena = match.arena();

    if (arena != null) {

      SlotOwner defenderOwner =

          session.attackerSide() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;

      ArenaPedalTarget target = new ArenaPedalTarget.Battle(defenderOwner, lane);

      arena.setPedalHighlight(target, BattleArena.PedalHighlight.SELECTED);

    }

    beamSound(match, session.attackerSide(), ArenaSlotSounds.Kind.SELECT);



    if (defender != null) {
      if (CombatModifiers.laneStrikeMode(match.board(), session.attackerSide(), attacker, defender)
          == CombatModifiers.LaneStrikeMode.CREATURE) {
        match.feedback()
            .actionBarInfo(
                "<green>已选定 <white>%s<green>，再次左键确认攻击"
                    .formatted(defender.displayName()));
      } else {
        match.feedback()
            .actionBarInfo(
                "<green>已选定第 %d 列（潜水/空袭直伤），再次左键确认".formatted(lane + 1));
      }
    } else {
      match.feedback().actionBarInfo("<green>已选定空槽 %d，再次左键确认直伤".formatted(lane + 1));
    }

    return true;

  }



  private static void confirmLaneStrike(

      InscriptionMatch match,

      BeamCombatSession session,

      BoardCreature attacker,

      int lane,

      BoardCreature defender) {

    clearPendingGlow(match, session);

    session.clearPendingSelection();

    match.setBeamSession(null);

    beamSound(match, session.attackerSide(), ArenaSlotSounds.Kind.CONFIRM);



    BattleArena arena = match.arena();

    if (arena != null) {

      SlotOwner defenderOwner =

          session.attackerSide() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;

      ArenaPedalTarget target = new ArenaPedalTarget.Battle(defenderOwner, lane);

      arena.flashPedal(

          target,

          BattleArena.PedalHighlight.CONFIRMED,

          BattleArena.PedalHighlight.NORMAL,

          FLASH_TICKS);

    }



    Runnable afterStrike =
        () -> {
          if (attacker.isDead() || match.shouldStopCombatSequence()) {
            session.onSlotComplete().run();
            return;
          }
          if (attacker.hasSigil(SigilId.DOUBLE_STRIKE) && session.strikeRound() == 0) {
            resumeBeamSession(match, session, attacker, 1);
            return;
          }
          advanceTriBeamOrFinish(match, session, attacker);
        };



    if (defender != null
        && CombatModifiers.laneStrikeMode(match.board(), session.attackerSide(), attacker, defender)
            == CombatModifiers.LaneStrikeMode.CREATURE) {

      session.resolver().resolveBeamOnTarget(session.attackerSide(), attacker, defender, afterStrike);

    } else {

      session.resolver().resolveBeamOnLane(session.attackerSide(), attacker, lane, afterStrike);

    }

  }



  private static void resumeBeamSession(
      InscriptionMatch match, BeamCombatSession session, BoardCreature attacker, int strikeRound) {
    if (session.isTriBeam()) {
      beginTriBeam(
          match,
          session.resolver(),
          session.attackerSide(),
          session.slotIndex(),
          attacker,
          session.triDefenderLanes(),
          session.triPhaseIndex(),
          strikeRound,
          session.onSlotComplete());
    } else {
      begin(
          match,
          session.resolver(),
          session.attackerSide(),
          session.slotIndex(),
          attacker,
          strikeRound,
          session.onSlotComplete());
    }
  }

  private static void advanceTriBeamOrFinish(
      InscriptionMatch match, BeamCombatSession session, BoardCreature attacker) {
    if (!session.isTriBeam()) {
      session.onSlotComplete().run();
      return;
    }
    int nextPhase = session.triPhaseIndex() + 1;
    if (nextPhase >= session.triDefenderLanes().length) {
      session.onSlotComplete().run();
      return;
    }
    beginTriBeam(
        match,
        session.resolver(),
        session.attackerSide(),
        session.slotIndex(),
        attacker,
        session.triDefenderLanes(),
        nextPhase,
        0,
        session.onSlotComplete());
  }

  private static String triPhaseLabel(int phaseIndex, int phaseCount) {
    if (phaseCount <= 1) {
      return "单列";
    }
    return switch (phaseIndex) {
      case 0 -> "左路";
      case 1 -> phaseCount == 3 ? "中路" : "右路";
      default -> "右路";
    };
  }

  private static void clearPendingGlow(InscriptionMatch match, BeamCombatSession session) {

    UUID pending = session.pendingTargetId();

    if (pending != null) {

      BoardCreature old = match.findCreatureByInstanceId(pending);

      if (old != null) {

        CreatureEntityService.setCombatGlow(old, false);

      }

    }

    Integer lane = session.pendingLaneIndex();

    if (lane != null && match.arena() != null) {

      SlotOwner defenderOwner =

          session.attackerSide() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;

      match.arena()

          .setPedalHighlight(

              new ArenaPedalTarget.Battle(defenderOwner, lane), BattleArena.PedalHighlight.NORMAL);

    }

  }



  static List<BoardCreature> listSelectableTargets(

      InscriptionMatch match, MatchSide attackerSide, BoardCreature attacker) {

    SlotOwner defenderOwner =

        attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;

    List<BoardCreature> out = new ArrayList<>();

    for (BoardSlot slot : match.board().row(defenderOwner)) {

      if (slot.isEmpty()) continue;

      BoardCreature defender = slot.creature();

      if (defender != null && isSelectableTarget(match, attackerSide, attacker, defender)) {

        out.add(defender);

      }

    }

    return out;

  }



  static boolean isSelectableTarget(

      InscriptionMatch match,

      MatchSide attackerSide,

      BoardCreature attacker,

      BoardCreature defender) {

    if (defender.owner() == attacker.owner()) return false;

    return CombatModifiers.laneStrikeMode(match.board(), attackerSide, attacker, defender)
        == CombatModifiers.LaneStrikeMode.CREATURE;

  }

  private static boolean isLaneStrikeable(
      InscriptionMatch match,
      MatchSide attackerSide,
      BoardCreature attacker,
      BoardCreature defenderOrNull) {
    return CombatModifiers.laneStrikeMode(match.board(), attackerSide, attacker, defenderOrNull)
        != CombatModifiers.LaneStrikeMode.NONE;
  }

  private static void beamSound(InscriptionMatch match, MatchSide side, ArenaSlotSounds.Kind kind) {
    if (match == null || side == null) {
      return;
    }
    match.participant(side).player().ifPresent(ext -> ArenaSlotSounds.play(ext.player(), kind));
  }

}

