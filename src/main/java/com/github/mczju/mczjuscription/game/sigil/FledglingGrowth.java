package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

import java.util.Map;

/** 幼雏：在场上经历完整战斗回合后成长。 */
public final class FledglingGrowth {

    /** 原生幼雏 → 成长形态（后续可配置扩展）。 */
    private static final Map<CardId, CardId> NATIVE_GROWTH = Map.of(
            CardId.WOLF_CUB, CardId.WOLF
    );

    private FledglingGrowth() {}

    public static void mature(InscriptionMatch match, BoardCreature creature) {
        BoardSlot slot = creature.slot();
        if (slot == null || creature.isDead()) {
            return;
        }

        CardId evolved = NATIVE_GROWTH.get(creature.cardId());
        if (evolved != null) {
            match.replaceWith(slot, evolved, creature.owner());
            return;
        }

        if (!creature.hasSigil(SigilId.FLEDGLING) || creature.hasMaturedFromFledgling()) {
            return;
        }
        creature.applyElderForm();
        CreatureEntityService.refreshLabel(creature);
    }
}
