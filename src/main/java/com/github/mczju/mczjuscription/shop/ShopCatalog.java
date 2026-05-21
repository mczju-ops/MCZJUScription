package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.card.CardId;
import java.util.EnumMap;
import java.util.Map;

/**
 * 旧版硬编码价目表；运行期商店以 {@link ShopConfigStorage} 为准。
 * 仅用于首次生成 shop.yml 时的迁移种子。
 */
public final class ShopCatalog {

  private static final Map<CardId, Integer> BONE_PRICES = new EnumMap<>(CardId.class);

  static {
    for (CardId id : CardId.shopSeedPool()) {
      BONE_PRICES.put(id, switch (id) {
        case RABBIT -> 0;
        case BEE -> 2;
        case WOLF_CUB -> 2;
        case WOLF -> 3;
        case ANT -> 1;
        default -> 2;
      });
    }
  }

  private ShopCatalog() {}

  public static int bonePrice(CardId cardId) {
    return legacyBonePrice(cardId);
  }

  public static int legacyBonePrice(CardId cardId) {
    return BONE_PRICES.getOrDefault(cardId, 2);
  }

  public static CardId[] availableCards() {
    return BONE_PRICES.keySet().toArray(CardId[]::new);
  }

  public static CardId[] legacyCardIds() {
    return availableCards();
  }
}
