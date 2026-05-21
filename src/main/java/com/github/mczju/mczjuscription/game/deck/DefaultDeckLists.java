package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import java.util.ArrayList;
import java.util.List;

public final class DefaultDeckLists {

  private DefaultDeckLists() {}

  /** 默认自由构牌（mob 卡，已移除旧 CardId 内置十卡）。 */
  public static List<String> starterFreeBuildDeck() {
    return List.of(
        "mob_wolf",
        "mob_wolf",
        "mob_rabbit",
        "mob_bee",
        "mob_wolf_cub");
  }

  public static List<String> parseDeck(List<String> raw) {
    List<String> deck = new ArrayList<>();
    if (raw == null) return deck;
    for (String name : raw) {
      if (name == null || name.isBlank()) continue;
      if (CardCatalog.exists(name)) {
        deck.add(name);
      }
    }
    return deck;
  }
}
