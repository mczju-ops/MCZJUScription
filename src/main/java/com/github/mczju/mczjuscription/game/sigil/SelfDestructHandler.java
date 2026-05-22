package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;

/** 【自爆】：死亡时对面前格与相邻两格上的造物造成 10 点伤害（空位无效）。 */
public final class SelfDestructHandler {

  private static final int DAMAGE = 10;
  private static final long SPLASH_DELAY_TICKS = 6L;

  private SelfDestructHandler() {}

  public static void explode(InscriptionMatch match, BoardCreature source) {
    if (!source.hasSigil(SigilId.SELF_DESTRUCT)) return;
    if (HissAura.suppressesSelfDestruct(match, source.owner())) return;
    BoardSlot slot = source.slot();
    if (slot == null) return;

    int lane = slot.index();
    SlotOwner owner = slot.owner();
    SlotOwner frontRow = owner == SlotOwner.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
    MatchSide bomberSide = source.owner();
    UUID bomberId = source.instanceId();

    List<BlastTarget> targets = new ArrayList<>(3);
    targets.add(new BlastTarget(frontRow, lane));
    for (int delta : new int[] {-1, 1}) {
      int idx = lane + delta;
      if (idx < 0 || idx >= BoardSlot.SLOT_COUNT) continue;
      targets.add(new BlastTarget(owner, idx));
    }

    Location mainAt = BoardVfx.locationOf(match, source);
    if (mainAt == null) {
      mainAt = BoardVfx.slotLocation(match, owner, lane);
    }
    if (mainAt != null) {
      BoardVfx.playSelfDestructMain(mainAt);
    }

    MCZJUScriptionPlugin plugin = MCZJUScriptionPlugin.getInstance();
    if (plugin == null) {
      applyBlast(match, targets, bomberSide, bomberId);
      return;
    }

    Bukkit.getScheduler()
        .runTaskLater(
            plugin,
            () -> {
              for (BlastTarget target : targets) {
                Location at = BoardVfx.slotLocation(match, target.row(), target.lane());
                if (at != null) {
                  BoardVfx.playSelfDestructSplash(at);
                }
              }
              applyBlast(match, targets, bomberSide, bomberId);
            },
            SPLASH_DELAY_TICKS);
  }

  private static void applyBlast(
      InscriptionMatch match, List<BlastTarget> targets, MatchSide bomberSide, UUID skipInstanceId) {
    for (BlastTarget target : targets) {
      damageLane(match, target.row(), target.lane(), bomberSide, skipInstanceId);
    }
  }

  private static void damageLane(
      InscriptionMatch match,
      SlotOwner rowOwner,
      int lane,
      MatchSide bomberSide,
      UUID skipInstanceId) {
    BoardSlot s = match.board().slot(rowOwner, lane);
    if (s.isEmpty() || s.creature() == null) {
      return;
    }
    BoardCreature c = s.creature();
    if (skipInstanceId != null && skipInstanceId.equals(c.instanceId())) {
      return;
    }
    if (c.absorbFirstHitWithShield()) return;
    match.damageCreature(c, DAMAGE, bomberSide);
  }

  private record BlastTarget(SlotOwner row, int lane) {}
}
