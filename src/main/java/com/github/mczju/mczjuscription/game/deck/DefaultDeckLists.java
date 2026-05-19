package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.card.CardId;

import java.util.ArrayList;
import java.util.List;

public final class DefaultDeckLists {

    private DefaultDeckLists() {}

    public static List<CardId> starterFreeBuildDeck() {
        return List.of(
                CardId.WOLF,
                CardId.WOLF,
                CardId.RABBIT,
                CardId.BEE,
                CardId.WOLF_CUB
        );
    }

    public static List<CardId> parseDeck(List<String> raw) {
        List<CardId> deck = new ArrayList<>();
        if (raw == null) return deck;
        for (String name : raw) {
            try {
                deck.add(CardId.valueOf(name));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return deck;
    }
}
