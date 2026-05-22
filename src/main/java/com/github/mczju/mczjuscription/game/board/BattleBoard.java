package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.MatchSide;

import java.util.ArrayList;
import java.util.List;

/**
 * 主战场 4 格 + 敌方准备区 4 格。
 */
public final class BattleBoard {

    private final BoardSlot[] playerSlots = createRow(SlotOwner.PLAYER);
    private final BoardSlot[] enemySlots = createRow(SlotOwner.ENEMY);
    private final BoardSlot[] enemyPreviewSlots = createRow(SlotOwner.ENEMY_PREVIEW);

    private static BoardSlot[] createRow(SlotOwner owner) {
        BoardSlot[] row = new BoardSlot[BoardSlot.SLOT_COUNT];
        for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
            row[i] = new BoardSlot(owner, i);
        }
        return row;
    }

    {
        for (BoardSlot slot : playerSlots) slot.attachBoard(this);
        for (BoardSlot slot : enemySlots) slot.attachBoard(this);
        for (BoardSlot slot : enemyPreviewSlots) slot.attachBoard(this);
    }

    public void refreshAllLabels() {
        CreatureEntityService.refreshBoardLabels(this);
    }

    public BoardSlot playerSlot(int index) {
        return playerSlots[index];
    }

    public BoardSlot enemySlot(int index) {
        return enemySlots[index];
    }

    public BoardSlot enemyPreviewSlot(int index) {
        return enemyPreviewSlots[index];
    }

    public BoardSlot[] row(SlotOwner owner) {
        return switch (owner) {
            case PLAYER -> playerSlots;
            case ENEMY -> enemySlots;
            case ENEMY_PREVIEW -> enemyPreviewSlots;
        };
    }

    public BoardSlot slot(SlotOwner owner, int index) {
        return row(owner)[index];
    }

    /**
     * 准备区造物前移进入主战场：仅当对应主战场格为空时前进；
     * 被挡住的预览区造物留在原位。
     *
     * @return 成功前移的数量
     */
    public int advanceEnemyPreviewCount() {
        int moved = 0;
        for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
            BoardSlot preview = enemyPreviewSlots[i];
            if (preview.isEmpty()) continue;
            if (!BoardRules.canAdvancePreviewToCombat(enemySlots, i)) continue;

            BoardCreature creature = preview.creature();
            CreatureEntityService.despawn(creature);
            preview.clear();
            creature.bind(enemySlots[i]);
            moved++;
        }
        return moved;
    }

    /** @deprecated 使用 {@link #advanceEnemyPreviewCount()} 并在每步后推进流水线 */
    @Deprecated
    public void advanceEnemyPreview() {
        advanceEnemyPreviewCount();
    }

    public void clearCombatRows() {
        for (BoardSlot slot : playerSlots) slot.clear();
        for (BoardSlot slot : enemySlots) slot.clear();
    }

    public List<BoardSlot> occupiedSlots(MatchSide side) {
        SlotOwner owner = side == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
        List<BoardSlot> result = new ArrayList<>();
        for (BoardSlot slot : row(owner)) {
            if (!slot.isEmpty()) result.add(slot);
        }
        return result;
    }
}
