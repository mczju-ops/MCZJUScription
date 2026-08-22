package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchParticipant;
import com.github.mczju.mczjuscription.shop.ShopPresenter;
import com.github.mczju.mczjuscription.shop.SimpleShopPresenter;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;

import java.util.List;

public final class DeckSourceFactory {

    private static final ShopPresenter DEFAULT_SHOP = new SimpleShopPresenter();

    private DeckSourceFactory() {}

    public static DeckSource create(DeckMode mode, MatchParticipant participant) {
        return switch (mode) {
            case FREE_BUILD -> new FreeBuildDeckSource(resolveFreeBuildDeck(participant));
            case SHOP -> new ShopDeckSource(DEFAULT_SHOP);
        };
    }

    private static List<String> resolveFreeBuildDeck(MatchParticipant participant) {
        if (participant.player() != null) {
            InscriptionPlayerData data = participant.player().getData(InscriptionPlayerData.class);
            List<String> saved = DefaultDeckLists.parseDeck(data.savedDeck);
            if (!saved.isEmpty()) return saved;
        }
        return DefaultDeckLists.starterFreeBuildDeck();
    }
}
