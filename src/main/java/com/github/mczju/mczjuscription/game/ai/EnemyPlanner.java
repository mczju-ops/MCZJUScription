package com.github.mczju.mczjuscription.game.ai;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/**
 * 敌方 AI：向准备区放置造物，回合结束时由 {@link BattleBoard#advanceEnemyPreview()} 落场。
 */
public final class EnemyPlanner {

    private final InscriptionMatch match;
    private int step;

    public EnemyPlanner(InscriptionMatch match) {
        this.match = match;
    }

    public void planNextPreview() {
        BattleBoard board = match.board();
        BoardSlot[] previewRow = board.row(com.github.mczju.mczjuscription.game.board.SlotOwner.ENEMY_PREVIEW);
        int slotIndex = BoardRules.nextEmptyIndex(previewRow, step % BoardSlot.SLOT_COUNT);
        if (slotIndex < 0) {
            step++;
            return;
        }
        BoardSlot preview = previewRow[slotIndex];
        BoardCreature creature = new BoardCreature(pickCard(), MatchSide.ENEMY);
        creature.bind(preview);
        match.spawnCreatureEntity(creature, com.github.mczju.mczjuscription.game.board.SlotOwner.ENEMY_PREVIEW, slotIndex);
        step++;
    }

    private CardId pickCard() {
        int turn = Math.max(1, match.turn().turnNumber());
        return switch ((step + turn) % 4) {
            case 0 -> CardId.WOLF;
            case 1 -> CardId.BEE;
            case 2 -> CardId.WOLF_CUB;
            default -> CardId.RABBIT;
        };
    }
}
