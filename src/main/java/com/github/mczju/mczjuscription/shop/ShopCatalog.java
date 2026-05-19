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
        BONE_PRICES.put(CardId.HARE, 1);
        BONE_PRICES.put(CardId.GUARDIAN, 3);
        BONE_PRICES.put(CardId.MOLE, 2);
        BONE_PRICES.put(CardId.GECKO, 2);
        BONE_PRICES.put(CardId.SKUNK, 2);
        BONE_PRICES.put(CardId.COYOTE, 3);
        BONE_PRICES.put(CardId.FROG, 2);
        BONE_PRICES.put(CardId.BAT, 2);
        BONE_PRICES.put(CardId.SHREW, 1);
        BONE_PRICES.put(CardId.MANTIS, 3);
        BONE_PRICES.put(CardId.BEAVER, 3);
        BONE_PRICES.put(CardId.GRIZZLY, 4);
        BONE_PRICES.put(CardId.GOAT, 2);
        BONE_PRICES.put(CardId.COCKROACH, 2);
        BONE_PRICES.put(CardId.MAGPIE, 2);
        BONE_PRICES.put(CardId.ADDER, 3);
        BONE_PRICES.put(CardId.BULL, 3);
        BONE_PRICES.put(CardId.PACK_RAT, 3);
        BONE_PRICES.put(CardId.GHOUL, 4);
        BONE_PRICES.put(CardId.AMALGAM, 4);
        BONE_PRICES.put(CardId.CAT, 2);
        BONE_PRICES.put(CardId.STUMP, 2);
        BONE_PRICES.put(CardId.BELL_SHEEP, 2);
    }

    private ShopCatalog() {}

    public static int bonePrice(CardId cardId) {
        return BONE_PRICES.getOrDefault(cardId, 2);
    }

    public static CardId[] availableCards() {
        return BONE_PRICES.keySet().toArray(CardId[]::new);
    }
}
