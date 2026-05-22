package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.deck.DeckSource;
import com.github.mczju.mczjuscription.game.match.Currency;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/** 某一方的运行时状态：资源、牌组、手牌、卡组逻辑。 */
public final class ParticipantState {

    private final MatchParticipant participant;
    private final @Nullable DeckSource deckSource;
    private final Currency currency = new Currency();
    private final Deque<String> mainDeck = new ArrayDeque<>();
    private final List<String> hand = new ArrayList<>();

    public ParticipantState(MatchParticipant participant, @Nullable DeckSource deckSource) {
        this.participant = participant;
        this.deckSource = deckSource;
    }

    public MatchSide side() {
        return participant.side();
    }

    public MatchParticipant participant() {
        return participant;
    }

    public @Nullable DeckSource deckSource() {
        return deckSource;
    }

    public Currency currency() {
        return currency;
    }

    public Deque<String> mainDeck() {
        return mainDeck;
    }

    public List<String> hand() {
        return hand;
    }

    public Optional<PlayerExt> player() {
        return Optional.ofNullable(participant.player());
    }

    public boolean isHuman() {
        return participant.isHuman();
    }
}
