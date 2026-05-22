package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.entity.CreatureAnimator;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

import java.util.ArrayList;
import java.util.List;

/** 敌方预览区造物按槽位顺序前移落场（带动画）。 */
public final class PreviewAdvanceSequence {

    private PreviewAdvanceSequence() {}

    public static void run(InscriptionMatch match, Runnable onComplete) {
        if (match.arena() == null) {
            int moved = match.board().advanceEnemyPreviewCount();
            for (int i = 0; i < moved; i++) {
                match.notifyEnemyBackfieldWaveAdvanced();
            }
            if (onComplete != null) onComplete.run();
            return;
        }

        List<Advance> steps = plan(match);
        if (steps.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }
        runStep(match, steps, 0, onComplete);
    }

    private static List<Advance> plan(InscriptionMatch match) {
        List<Advance> steps = new ArrayList<>();
        BattleBoard board = match.board();
        for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
            BoardSlot preview = board.enemyPreviewSlot(i);
            if (preview.isEmpty()) continue;
            if (!BoardRules.canAdvancePreviewToCombat(board.row(SlotOwner.ENEMY), i)) continue;

            var from = match.arena().slotLocation(SlotOwner.ENEMY_PREVIEW, i);
            var to = match.arena().slotLocation(SlotOwner.ENEMY, i);
            if (from == null || to == null) continue;

            steps.add(new Advance(preview.creature(), i, from, to));
        }
        return steps;
    }

    private static void runStep(InscriptionMatch match, List<Advance> steps, int index, Runnable onComplete) {
        if (index >= steps.size()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        Advance step = steps.get(index);
        var faceToward = match.arena() != null
                ? ArenaFacing.facingTarget(match.arena(), SlotOwner.ENEMY, step.slotIndex)
                : null;
        CreatureAnimator.playMoveSequence(
                step.creature,
                step.from,
                step.to,
                faceToward,
                () -> {
                    finalizeAdvance(match, step);
                    CreatureAnimator.schedule(
                            () -> runStep(match, steps, index + 1, onComplete),
                            CreatureAnimator.PAUSE_TICKS
                    );
                }
        );
    }

    private static void finalizeAdvance(InscriptionMatch match, Advance step) {
        BoardSlot preview = match.board().enemyPreviewSlot(step.slotIndex);
        if (!preview.isEmpty() && preview.creature() == step.creature) {
            preview.clear();
        }
        step.creature.bind(match.board().enemySlot(step.slotIndex));
        match.notifyEnemyBackfieldWaveAdvanced();
    }

    private record Advance(BoardCreature creature, int slotIndex, org.bukkit.Location from, org.bukkit.Location to) {}
}
