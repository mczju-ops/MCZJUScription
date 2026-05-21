package com.github.mczju.mczjuscription.game.combat;

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

/** 【射线】：玩家用献祭之剑右键敌方造物选定并确认攻击目标。 */
public final class BeamTargeting {

  private BeamTargeting() {}

  public static void begin(
      InscriptionMatch match,
      CombatResolver resolver,
      MatchSide attackerSide,
      int slotIndex,
      BoardCreature attacker,
      int strikeRound,
      Runnable onSlotComplete) {
    cancel(match);

    List<BoardCreature> targets = listSelectableTargets(match, attackerSide, attacker);
    if (targets.isEmpty()) {
      resolver.resolveBeamWithoutTarget(attackerSide, attacker, onSlotComplete);
      return;
    }

    BeamCombatSession session =
        new BeamCombatSession(
            resolver, attackerSide, slotIndex, attacker, strikeRound, onSlotComplete);
    match.setBeamSession(session);

    match.feedback()
        .announceInfo(
            "<gold>【%s】<white>：请用献祭之剑为我指定攻击目标"
                .formatted(attacker.displayName()));
    match.feedback()
        .actionBarInfo("<yellow>右键敌方造物选定；再次右键同一造物确认攻击");
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

    if (!isSelectableTarget(match, session.attackerSide(), attacker, clicked)) {
      match.feedback().actionBarWarn("<red>无法攻击该造物（震慑或攻击力为 0）");
      return true;
    }

    UUID pending = session.pendingTargetId();
    if (pending != null && pending.equals(clicked.instanceId())) {
      confirmAndStrike(match, session, attacker, clicked);
    } else {
      if (pending != null) {
        BoardCreature old = match.findCreatureByInstanceId(pending);
        if (old != null) {
          CreatureEntityService.setCombatGlow(old, false);
        }
      }
      session.setPendingTargetId(clicked.instanceId());
      CreatureEntityService.setCombatGlow(clicked, true);
      match.feedback()
          .actionBarInfo(
              "<green>已选定 <white>%s<green>，再次右键确认攻击"
                  .formatted(clicked.displayName()));
    }
    return true;
  }

  public static void cancel(InscriptionMatch match) {
    BeamCombatSession session = match.beamSession();
    if (session == null) {
      return;
    }
    UUID pending = session.pendingTargetId();
    if (pending != null) {
      BoardCreature old = match.findCreatureByInstanceId(pending);
      if (old != null) {
        CreatureEntityService.setCombatGlow(old, false);
      }
    }
    match.setBeamSession(null);
  }

  private static void confirmAndStrike(
      InscriptionMatch match,
      BeamCombatSession session,
      BoardCreature attacker,
      BoardCreature defender) {
    CreatureEntityService.setCombatGlow(defender, false);
    session.setPendingTargetId(null);
    match.setBeamSession(null);

    Runnable afterStrike =
        () -> {
          if (attacker.isDead() || match.isMatchOver()) {
            session.onSlotComplete().run();
            return;
          }
          if (attacker.hasSigil(SigilId.DOUBLE_STRIKE) && session.strikeRound() == 0) {
            begin(
                match,
                session.resolver(),
                session.attackerSide(),
                session.slotIndex(),
                attacker,
                1,
                session.onSlotComplete());
          } else {
            session.onSlotComplete().run();
          }
        };

    session.resolver().resolveBeamOnTarget(session.attackerSide(), attacker, defender, afterStrike);
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
    if (CombatModifiers.preventsAttack(defender)) return false;
    return CombatModifiers.effectiveAttack(match.board(), attacker, defender) > 0;
  }
}
