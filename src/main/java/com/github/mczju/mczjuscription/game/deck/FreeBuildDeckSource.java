package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.ParticipantState;

import java.util.List;

/** 自由构建：开局装入主牌组，抽牌阶段从牌组顶抽一张。 */
public final class FreeBuildDeckSource implements DeckSource {

    private final List<CardId> initialDeck;

    public FreeBuildDeckSource(List<CardId> initialDeck) {
        this.initialDeck = List.copyOf(initialDeck);
    }

    @Override
    public DeckMode mode() {
        return DeckMode.FREE_BUILD;
    }

    @Override
    public void initializeDeck(ParticipantState state, InscriptionMatch match) {
        state.mainDeck().addAll(initialDeck);
    }

    @Override
    public void performMainDrawAction(InscriptionMatch match, MatchSide side) {
        ParticipantState state = match.participant(side);
        if (state.mainDeck().isEmpty()) {
            match.feedback().actionBarWarn("<yellow>主牌组已空");
            return;
        }
        CardId drawn = state.mainDeck().removeFirst();
        match.grantCardToHand(side, drawn);
        match.completeMainDraw(side);
    }

    @Override
    public String mainDrawItemHint() {
        return "<gray>右键从主牌组抽一张牌";
    }
}
