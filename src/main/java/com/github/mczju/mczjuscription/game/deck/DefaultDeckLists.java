package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardId;
import java.util.ArrayList;
import java.util.List;

public final class DefaultDeckLists {

  private DefaultDeckLists() {}

  public static List<String> starterFreeBuildDeck() {
    return List.of(
        CardId.WOLF.name(),
        CardId.WOLF.name(),
        CardId.RABBIT.name(),
        CardId.BEE.name(),
        CardId.WOLF_CUB.name());
  }

  public static List<String> parseDeck(List<String> raw) {
    List<String> deck = new ArrayList<>();
    if (raw == null) return deck;
    for (String name : raw) {
      if (name == null || name.isBlank()) continue;
      if (CardCatalog.exists(name)) {
        deck.add(name);
        continue;
      }
      try {
        CardId id = CardId.valueOf(name.trim().toUpperCase());
        deck.add(id.name());
      } catch (IllegalArgumentException ignored) {
      }
    }
    return deck;
  }
}
