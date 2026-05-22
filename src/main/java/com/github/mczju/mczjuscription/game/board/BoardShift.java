package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.entity.CreatureAnimator;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * 场面位移（左冲 / 右冲 / 推挤 / 随机换格）。不同印记可指定移动表现。
 */
public final class BoardShift {

  /** 换格时的移动表现：Instant=瞬移重生，Walk=走过去，BreezeJump=旋风人跳跃，Ender=末影人传送。 */
  public enum ShiftStyle {
    INSTANT,
    WALK,
    BREEZE_JUMP,
    ENDER_TELEPORT
  }

  private BoardShift() {}

  /** 移动到指定列（目标格须为空）。 */
  public static boolean moveToIndex(
      InscriptionMatch match, BoardCreature creature, int targetIndex) {
    return moveToIndex(match, creature, targetIndex, ShiftStyle.INSTANT);
  }

  public static boolean moveToIndex(
      InscriptionMatch match, BoardCreature creature, int targetIndex, ShiftStyle style) {
    BoardSlot from = creature.slot();
    if (from == null || from.index() == targetIndex) return false;
    if (targetIndex < 0 || targetIndex >= BoardSlot.SLOT_COUNT) return false;
    BoardSlot[] row = match.board().row(from.owner());
    BoardSlot to = row[targetIndex];
    if (!to.isEmpty()) return false;
    return relocate(match, creature, from, to, style);
  }

  public static boolean moveRandomEmpty(InscriptionMatch match, BoardCreature creature) {
    return moveRandomEmpty(match, creature, ShiftStyle.INSTANT);
  }

  public static boolean moveRandomEmpty(
      InscriptionMatch match, BoardCreature creature, ShiftStyle style) {
    BoardSlot from = creature.slot();
    if (from == null) return false;
    List<Integer> empty = new ArrayList<>();
    BoardSlot[] row = match.board().row(from.owner());
    for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
      if (row[i].isEmpty()) empty.add(i);
    }
    if (empty.isEmpty()) return false;
    int target = empty.get(ThreadLocalRandom.current().nextInt(empty.size()));
    return moveToIndex(match, creature, target, style);
  }

  public static boolean move(InscriptionMatch match, BoardCreature creature, int delta) {
    BoardSlot from = creature.slot();
    if (from == null || delta == 0) return false;
    int targetIndex = from.index() + delta;
    if (targetIndex < 0 || targetIndex >= BoardSlot.SLOT_COUNT) return false;

    BoardSlot[] row = match.board().row(from.owner());
    BoardSlot to = row[targetIndex];
    if (to.isEmpty()) {
      return relocate(match, creature, from, to, ShiftStyle.INSTANT);
    }
    if (creature.hasSigil(SigilId.RUSH_PUSH)) {
      BoardCreature blocker = to.creature();
      if (blocker == null) return false;
      int pushTo = targetIndex + delta;
      if (pushTo < 0 || pushTo >= BoardSlot.SLOT_COUNT) return false;
      if (!row[pushTo].isEmpty()) return false;
      relocate(match, blocker, to, row[pushTo], ShiftStyle.INSTANT);
      return relocate(match, creature, from, to, ShiftStyle.INSTANT);
    }
    return false;
  }

  private static boolean relocate(
      InscriptionMatch match,
      BoardCreature creature,
      BoardSlot from,
      BoardSlot to,
      ShiftStyle style) {
    if (match.arena() == null) {
      from.clear();
      creature.bind(to);
      return true;
    }

    Location fromLoc = match.arena().slotLocation(from.owner(), from.index());
    Location toLoc = match.arena().slotLocation(to.owner(), to.index());
    if (fromLoc == null || toLoc == null) {
      from.clear();
      creature.bind(to);
      return true;
    }

    return switch (style) {
      case WALK -> relocateWalk(match, creature, from, to, fromLoc, toLoc);
      case BREEZE_JUMP -> relocateBreezeJump(match, creature, from, to, fromLoc, toLoc);
      case ENDER_TELEPORT -> relocateEnderTeleport(match, creature, from, to, fromLoc, toLoc);
      case INSTANT -> relocateInstant(match, creature, from, to, fromLoc, toLoc);
    };
  }

  private static boolean relocateInstant(
      InscriptionMatch match,
      BoardCreature creature,
      BoardSlot from,
      BoardSlot to,
      Location fromLoc,
      Location toLoc) {
    BoardVfx.playMove(fromLoc, toLoc);
    CreatureEntityService.despawn(creature);
    from.clear();
    creature.bind(to);
    match.spawnCreatureEntity(creature, to.owner(), to.index());
    return true;
  }

  private static boolean relocateWalk(
      InscriptionMatch match,
      BoardCreature creature,
      BoardSlot from,
      BoardSlot to,
      Location fromLoc,
      Location toLoc) {
    from.clear();
    creature.bind(to);
    Location faceToward = opponentSlot(match, from.owner(), to.index());
    if (hasLiveEntity(creature)) {
      CreatureAnimator.playMoveSequence(creature, fromLoc, toLoc, faceToward, () -> {});
    } else {
      BoardVfx.playMove(fromLoc, toLoc);
      match.spawnCreatureEntity(creature, to.owner(), to.index());
    }
    return true;
  }

  private static boolean relocateBreezeJump(
      InscriptionMatch match,
      BoardCreature creature,
      BoardSlot from,
      BoardSlot to,
      Location fromLoc,
      Location toLoc) {
    from.clear();
    creature.bind(to);
    if (hasLiveEntity(creature)) {
      CreatureAnimator.playBreezeJump(creature, fromLoc, toLoc, () -> {});
    } else {
      BoardVfx.playMove(fromLoc, toLoc);
      match.spawnCreatureEntity(creature, to.owner(), to.index());
    }
    return true;
  }

  private static boolean relocateEnderTeleport(
      InscriptionMatch match,
      BoardCreature creature,
      BoardSlot from,
      BoardSlot to,
      Location fromLoc,
      Location toLoc) {
    BoardVfx.playEnderTeleport(fromLoc);
    from.clear();
    creature.bind(to);
    Location end = CreatureAnimator.slotStand(creature, toLoc);
    if (hasLiveEntity(creature)) {
      snapEntity(creature, end);
    } else {
      match.spawnCreatureEntity(creature, to.owner(), to.index());
    }
    BoardVfx.playEnderTeleport(toLoc);
    return true;
  }

  private static Location opponentSlot(InscriptionMatch match, SlotOwner owner, int lane) {
    SlotOwner opponent = owner == SlotOwner.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    if (match.arena() == null) return null;
    return match.arena().slotLocation(opponent, lane);
  }

  private static boolean hasLiveEntity(BoardCreature creature) {
    if (creature.entityId() == null) return false;
    Entity entity = Bukkit.getEntity(creature.entityId());
    return entity != null && entity.isValid();
  }

  private static void snapEntity(BoardCreature creature, Location end) {
    if (end == null) return;
    Entity body = creature.entityId() == null ? null : Bukkit.getEntity(creature.entityId());
    if (body == null || !body.isValid()) return;

    Location goal = end.clone();
    goal.setYaw(body.getLocation().getYaw());
    goal.setPitch(body.getLocation().getPitch());
    body.teleport(goal);

    if (creature.displayEntityId() != null) {
      Entity label = Bukkit.getEntity(creature.displayEntityId());
      if (label != null && label.isValid()) {
        double labelOffset =
            body instanceof LivingEntity living ? living.getHeight() + 0.35 : 0.55;
        label.teleport(goal.clone().add(0, labelOffset, 0));
      }
    }
  }
}
