package com.github.mczju.mczjuscription.game.wave;

import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * 敌方出怪流水线：顶栏预览 → 后场 → 站场。
 * <ul>
 *   <li>开局先 roll 3 只：第 1 只进后场，第 2 只显示在顶栏，第 3 只暂存</li>
 *   <li>后场造物每前进到站场一步：再 roll 1 只，顶栏补进后场，队首补进顶栏</li>
 *   <li>顶栏展示的是「下一只将要进后场的」模板，与后场当前怪一致对齐</li>
 * </ul>
 */
public final class EnemyWaveDirector {

    private static final int INITIAL_ROLLS = 3;

    private final EnemyWaveSource source;
    /** 顶栏 2×11 展示的模板（下一只进后场）。 */
    private String stripTemplate;
    /** 已 roll、尚未上顶栏的队列。 */
    private final Deque<String> pending = new ArrayDeque<>();

    public EnemyWaveDirector(EnemyWaveSource source) {
        this.source = source;
    }

    public void onMatchStart(InscriptionMatch match) {
        source.onMatchStart(match);
        stripTemplate = null;
        pending.clear();
        for (int i = 0; i < INITIAL_ROLLS; i++) {
            pending.addLast(source.nextCreature(match));
        }
        bootstrapPipeline(match);
    }

    /** 小局清场后：按当前队列恢复后场与顶栏，避免 deployIndex 卡死不再出怪。 */
    public void onRoundReset(InscriptionMatch match) {
        bootstrapPipeline(match);
    }

    /** 回合末：后场有空位时把顶栏队首部署进去。 */
    public void deployOneToBackfield(InscriptionMatch match) {
        tryDeployStripToBackfield(match);
    }

    /** 后场→站场每推进一只后调用：roll 并入队，顶栏→后场，队首→顶栏。 */
    public void onPipelineStep(InscriptionMatch match) {
        pending.addLast(source.nextCreature(match));
        tryDeployStripToBackfield(match);
        promoteQueueHeadToStrip();
        refreshStrip(match);
    }

    /** 顶栏应展示的模板 id（下一只进后场）。 */
    public List<String> stripPreviewTemplates() {
        if (stripTemplate == null) {
            return List.of();
        }
        return Collections.singletonList(stripTemplate);
    }

    private void bootstrapPipeline(InscriptionMatch match) {
        if (backRowHasEmpty(match) && isBackRowEmpty(match)) {
            if (stripTemplate != null) {
                tryDeployStripToBackfield(match);
            } else if (!pending.isEmpty()) {
                deployToBackfield(match, pending.pollFirst());
            }
        }
        promoteQueueHeadToStrip();
        refreshStrip(match);
    }

    private void tryDeployStripToBackfield(InscriptionMatch match) {
        if (stripTemplate == null || !backRowHasEmpty(match)) {
            return;
        }
        String deploying = stripTemplate;
        stripTemplate = null;
        deployToBackfield(match, deploying);
        promoteQueueHeadToStrip();
    }

    private void promoteQueueHeadToStrip() {
        if (stripTemplate == null && !pending.isEmpty()) {
            stripTemplate = pending.pollFirst();
        }
    }

    private void deployToBackfield(InscriptionMatch match, String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return;
        }
        BoardSlot[] previewRow = match.board().row(SlotOwner.ENEMY_PREVIEW);
        int slotIndex = BoardRules.nextEmptyIndex(previewRow, 0);
        if (slotIndex < 0) {
            pending.addFirst(templateId);
            return;
        }
        BoardSlot preview = previewRow[slotIndex];
        BoardCreature creature = new BoardCreature(templateId, MatchSide.ENEMY);
        creature.bind(preview);
        match.spawnCreatureEntity(creature, SlotOwner.ENEMY_PREVIEW, slotIndex);
    }

    private static boolean backRowHasEmpty(InscriptionMatch match) {
        return BoardRules.nextEmptyIndex(match.board().row(SlotOwner.ENEMY_PREVIEW), 0) >= 0;
    }

    private static boolean isBackRowEmpty(InscriptionMatch match) {
        for (BoardSlot slot : match.board().row(SlotOwner.ENEMY_PREVIEW)) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void refreshStrip(InscriptionMatch match) {
        if (match.arena() != null) {
            match.arena().refreshPreviewStrip(match);
        }
    }
}
