package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import org.bukkit.Location;

/**
 * 场面位移（左冲 / 右冲 / 推挤）。Phase 3 印记使用。
 */
public final class BoardShift {

  private BoardShift() {}

  /**
   * 将造物向 {@code delta} 方向移动一格（-1 左，+1 右）。目标格须为空或可被推开。
   *
   * @return 是否发生了移动
   */
  /** 移动到指定列（目标格须为空）。 */
  public static boolean moveToIndex(
      InscriptionMatch match, BoardCreature creature, int targetIndex) {
    BoardSlot from = creature.slot();
    if (from == null || from.index() == targetIndex) return false;
    if (targetIndex < 0 || targetIndex >= BoardSlot.SLOT_COUNT) return false;
    BoardSlot[] row = match.board().row(from.owner());
    BoardSlot to = row[targetIndex];
    if (!to.isEmpty()) return false;
    return relocate(match, creature, from, to);
  }

  /** 随机移动到己方任一空位（末影人【穿梭】、旋风人【蓄风】等）。 */
  public static boolean moveRandomEmpty(InscriptionMatch match, BoardCreature creature) {
    BoardSlot from = creature.slot();
    if (from == null) return false;
    java.util.List<Integer> empty = new java.util.ArrayList<>();
    BoardSlot[] row = match.board().row(from.owner());
    for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
      if (row[i].isEmpty()) empty.add(i);
    }
    if (empty.isEmpty()) return false;
    int target = empty.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(empty.size()));
    return moveToIndex(match, creature, target);
  }

  public static boolean move(InscriptionMatch match, BoardCreature creature, int delta) {
    BoardSlot from = creature.slot();
    if (from == null || delta == 0) return false;
    int targetIndex = from.index() + delta;
    if (targetIndex < 0 || targetIndex >= BoardSlot.SLOT_COUNT) return false;

    BoardSlot[] row = match.board().row(from.owner());
    BoardSlot to = row[targetIndex];
    if (to.isEmpty()) {
      return relocate(match, creature, from, to);
    }
    if (creature.hasSigil(SigilId.RUSH_PUSH)) {
      BoardCreature blocker = to.creature();
      if (blocker == null) return false;
      int pushTo = targetIndex + delta;
      if (pushTo < 0 || pushTo >= BoardSlot.SLOT_COUNT) return false;
      if (!row[pushTo].isEmpty()) return false;
      relocate(match, blocker, to, row[pushTo]);
      return relocate(match, creature, from, to);
    }
    return false;
  }

  private static boolean relocate(
      InscriptionMatch match, BoardCreature creature, BoardSlot from, BoardSlot to) {
    if (match.arena() != null) {
      Location a = match.arena().slotLocation(from.owner(), from.index());
      Location b = match.arena().slotLocation(to.owner(), to.index());
      if (a != null && b != null) {
        BoardVfx.playMove(a, b);
      }
    }
    CreatureEntityService.despawn(creature);
    from.clear();
    creature.bind(to);
    if (match.arena() != null) {
      match.spawnCreatureEntity(creature, to.owner(), to.index());
    }
    return true;
  }
}
