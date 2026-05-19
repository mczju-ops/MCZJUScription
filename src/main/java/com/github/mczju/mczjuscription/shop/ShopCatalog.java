package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.card.CardId;

import java.util.EnumMap;
import java.util.Map;

/** 商店可购卡牌及骨币价格（占位数值，后续可配置化）。 */
public final class ShopCatalog {

    private static final Map<CardId, Integer> BONE_PRICES = new EnumMap<>(CardId.class);

    static {
        BONE_PRICES.put(CardId.RABBIT, 0);
        BONE_PRICES.put(CardId.BEE, 2);
        BONE_PRICES.put(CardId.CHICKEN, 2);
        BONE_PRICES.put(CardId.WOLF_CUB, 2);
        BONE_PRICES.put(CardId.WOLF, 3);
        BONE_PRICES.put(CardId.BONE_LORD, 6);
    }

    private ShopCatalog() {}

    public static int bonePrice(CardId cardId) {
        return BONE_PRICES.getOrDefault(cardId, 2);
    }

    public static CardId[] availableCards() {
        return BONE_PRICES.keySet().toArray(CardId[]::new);
    }
}
