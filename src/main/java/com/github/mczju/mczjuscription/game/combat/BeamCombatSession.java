package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import java.util.UUID;

/** 玩家【射线】选目标会话（献祭之剑右键选定 / 确认）。 */
public final class BeamCombatSession {

  private final CombatResolver resolver;
  private final MatchSide attackerSide;
  private final int slotIndex;
  private final UUID attackerId;
  private final int strikeRound;
  private final Runnable onSlotComplete;

  private UUID pendingTargetId;
  /** 射线选中的敌方列（含空槽直伤）。 */
  private Integer pendingLaneIndex;

  BeamCombatSession(
      CombatResolver resolver,
      MatchSide attackerSide,
      int slotIndex,
      BoardCreature attacker,
      int strikeRound,
      Runnable onSlotComplete) {
    this.resolver = resolver;
    this.attackerSide = attackerSide;
    this.slotIndex = slotIndex;
    this.attackerId = attacker.instanceId();
    this.strikeRound = strikeRound;
    this.onSlotComplete = onSlotComplete;
  }

  public CombatResolver resolver() {
    return resolver;
  }

  public MatchSide attackerSide() {
    return attackerSide;
  }

  public int slotIndex() {
    return slotIndex;
  }

  public UUID attackerId() {
    return attackerId;
  }

  public int strikeRound() {
    return strikeRound;
  }

  public Runnable onSlotComplete() {
    return onSlotComplete;
  }

  public UUID pendingTargetId() {
    return pendingTargetId;
  }

  public void setPendingTargetId(UUID pendingTargetId) {
    this.pendingTargetId = pendingTargetId;
  }

  public Integer pendingLaneIndex() {
    return pendingLaneIndex;
  }

  public void setPendingLaneIndex(Integer pendingLaneIndex) {
    this.pendingLaneIndex = pendingLaneIndex;
  }

  public void clearPendingSelection() {
    this.pendingTargetId = null;
    this.pendingLaneIndex = null;
  }
}
