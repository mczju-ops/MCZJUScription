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

  /** 游荡：在左/右相邻格中随机选一方向移动（空位直走；【蛮力】可推挤再占格）。 */
  public static boolean moveRandomAdjacent(
      InscriptionMatch match, BoardCreature creature, ShiftStyle style) {
    BoardSlot from = creature.slot();
    if (from == null) return false;
    BoardSlot[] row = match.board().row(from.owner());
    List<Integer> deltas = new ArrayList<>();
    for (int delta : new int[] {-1, 1}) {
      int targetIndex = from.index() + delta;
      if (targetIndex < 0 || targetIndex >= BoardSlot.SLOT_COUNT) continue;
      if (row[targetIndex].isEmpty()) {
        deltas.add(delta);
      } else if (creature.hasSigil(SigilId.RUSH_PUSH)) {
        int pushTo = targetIndex + delta;
        if (pushTo >= 0 && pushTo < BoardSlot.SLOT_COUNT && row[pushTo].isEmpty()) {
          deltas.add(delta);
        }
      }
    }
    if (deltas.isEmpty()) return false;
    int delta = deltas.get(ThreadLocalRandom.current().nextInt(deltas.size()));
    return move(match, creature, delta, style);
  }

  public static boolean move(InscriptionMatch match, BoardCreature creature, int delta) {
    return move(match, creature, delta, ShiftStyle.INSTANT);
  }

  public static boolean move(
      InscriptionMatch match, BoardCreature creature, int delta, ShiftStyle style) {
    BoardSlot from = creature.slot();
    if (from == null || delta == 0) return false;
    int targetIndex = from.index() + delta;
    if (targetIndex < 0 || targetIndex >= BoardSlot.SLOT_COUNT) return false;

    BoardSlot[] row = match.board().row(from.owner());
    BoardSlot to = row[targetIndex];
    if (to.isEmpty()) {
      return relocate(match, creature, from, to, style);
    }
    if (creature.hasSigil(SigilId.RUSH_PUSH)) {
      BoardCreature blocker = to.creature();
      if (blocker == null) return false;
      int pushTo = targetIndex + delta;
      if (pushTo < 0 || pushTo >= BoardSlot.SLOT_COUNT) return false;
      if (!row[pushTo].isEmpty()) return false;
      return relocatePushThenWalk(match, creature, blocker, from, to, row[pushTo], style);
    }
    return false;
  }

  /** 【蛮力】推挤相邻造物后，推挤者占入该格；先播被推挤者滑出，再播推挤者走入。 */
  private static boolean relocatePushThenWalk(
      InscriptionMatch match,
      BoardCreature pusher,
      BoardCreature pushed,
      BoardSlot pusherFrom,
      BoardSlot middle,
      BoardSlot pushDest,
      ShiftStyle pusherStyle) {
    if (match.arena() == null) {
      pushed.bind(pushDest);
      pusher.bind(middle);
      return true;
    }

    int middleIdx = middle.index();
    int pusherFromIdx = pusherFrom.index();
    SlotOwner rowOwner = middle.owner();

    Location pusherFromLoc = match.arena().slotLocation(rowOwner, pusherFromIdx);
    Location middleLoc = match.arena().slotLocation(rowOwner, middleIdx);
    Location pushDestLoc =
        match.arena().slotLocation(pushDest.owner(), pushDest.index());
    if (pusherFromLoc == null || middleLoc == null || pushDestLoc == null) {
      pushed.bind(pushDest);
      pusher.bind(middle);
      return true;
    }

    pushed.bind(pushDest);
    pusher.bind(middle);

    Location faceToward = opponentSlot(match, rowOwner, middleIdx);
    Runnable walkPusher =
        () -> {
          if (hasLiveEntity(pusher)) {
            if (pusherStyle == ShiftStyle.WALK) {
              CreatureAnimator.playMoveSequence(
                  pusher, pusherFromLoc, middleLoc, faceToward, () -> match.refreshCreatureLabels());
            } else {
              BoardVfx.playMove(pusherFromLoc, middleLoc, 6);
              CreatureEntityService.snapToBoardSlot(match, pusher, rowOwner, middleIdx, middleLoc);
              match.refreshCreatureLabels();
            }
          } else {
            BoardVfx.playMove(pusherFromLoc, middleLoc, CreatureAnimator.MOVE_TICKS + 4);
            match.spawnCreatureEntity(pusher, rowOwner, middleIdx);
            match.refreshCreatureLabels();
          }
        };

    if (hasLiveEntity(pushed)) {
      CreatureEntityService.snapToBoardSlot(match, pushed, rowOwner, middleIdx, middleLoc);
      CreatureAnimator.playPushSlide(
          pushed,
          middleLoc,
          pushDestLoc,
          () -> {
            CreatureEntityService.snapToBoardSlot(match, pushed);
            if (hasLiveEntity(pusher)) {
              CreatureEntityService.snapToBoardSlot(
                  match, pusher, rowOwner, pusherFromIdx, pusherFromLoc);
            }
            walkPusher.run();
          });
    } else {
      BoardVfx.playPushImpact(pushDestLoc);
      match.spawnCreatureEntity(pushed, pushDest.owner(), pushDest.index());
      walkPusher.run();
    }
    return true;
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
    BoardVfx.playMove(fromLoc, toLoc, 6);
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
    creature.bind(to);
    Location faceToward = opponentSlot(match, from.owner(), to.index());
    if (hasLiveEntity(creature)) {
      CreatureAnimator.playMoveSequence(
          creature, fromLoc, toLoc, faceToward, () -> match.refreshCreatureLabels());
    } else {
      BoardVfx.playMove(fromLoc, toLoc, CreatureAnimator.MOVE_TICKS + 4);
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
      CreatureAnimator.playBreezeJump(
          creature,
          fromLoc,
          toLoc,
          () -> {
            CreatureEntityService.snapToBoardSlot(match, creature);
            match.refreshCreatureLabels();
          });
    } else {
      BoardVfx.playMove(fromLoc, toLoc, CreatureAnimator.MOVE_TICKS + 4);
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
      CreatureEntityService.snapToBoardSlot(match, creature, to.owner(), to.index(), toLoc);
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
}
