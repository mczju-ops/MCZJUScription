package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.deck.DeckSource;
import com.github.mczju.mczjuscription.game.deck.DeckSourceFactory;
import com.github.mczju.mczjuscription.game.match.MatchSide;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 开局配置：2×2 变体、参与者、蜡烛、AI、各方卡组来源。 */
public final class MatchSetup {

    private final PlayVariant variant;
    private final List<MatchParticipant> participants;
    private final Map<MatchSide, Integer> candlesBySide;
    private final OpponentController opponentController;
    private final Map<MatchSide, DeckSource> deckSources;

    public MatchSetup(
            PlayVariant variant,
            List<MatchParticipant> participants,
            Map<MatchSide, Integer> candlesBySide,
            OpponentController opponentController
    ) {
        this.variant = variant;
        this.participants = List.copyOf(participants);
        this.candlesBySide = Map.copyOf(candlesBySide);
        this.opponentController = opponentController;
        this.deckSources = buildDeckSources(participants, variant.deckMode());
    }

    private static Map<MatchSide, DeckSource> buildDeckSources(List<MatchParticipant> participants, DeckMode deckMode) {
        Map<MatchSide, DeckSource> map = new EnumMap<>(MatchSide.class);
        for (MatchParticipant p : participants) {
            if (!p.isAiControlled()) {
                map.put(p.side(), DeckSourceFactory.create(deckMode, p));
            }
        }
        return map;
    }

    public PlayVariant variant() {
        return variant;
    }

    /** @deprecated 使用 {@link #variant()} */
    public MatchMode mode() {
        return variant.matchMode();
    }

    public DeckMode deckMode() {
        return variant.deckMode();
    }

    public List<MatchParticipant> participants() {
        return participants;
    }

    public int candlesFor(MatchSide side) {
        return candlesBySide.getOrDefault(side, 0);
    }

    public OpponentController opponentController() {
        return opponentController;
    }

    public DeckSource deckSource(MatchSide side) {
        return deckSources.get(side);
    }

    public Map<MatchSide, ParticipantState> createParticipantStates() {
        Map<MatchSide, ParticipantState> states = new EnumMap<>(MatchSide.class);
        for (MatchParticipant p : participants) {
            states.put(p.side(), new ParticipantState(p, deckSources.get(p.side())));
        }
        return states;
    }
}
