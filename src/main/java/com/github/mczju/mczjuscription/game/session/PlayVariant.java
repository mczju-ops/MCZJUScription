package com.github.mczju.mczjuscription.game.session;

/**
 * 2×2 玩法变体：{@link MatchMode} × {@link DeckMode}。
 */
public record PlayVariant(MatchMode matchMode, DeckMode deckMode) {

    public static final PlayVariant SOLO_FREE_BUILD = new PlayVariant(MatchMode.SOLO_PVE, DeckMode.FREE_BUILD);
    public static final PlayVariant SOLO_SHOP = new PlayVariant(MatchMode.SOLO_PVE, DeckMode.SHOP);
    public static final PlayVariant DUEL_FREE_BUILD = new PlayVariant(MatchMode.DUEL_PVP, DeckMode.FREE_BUILD);
    public static final PlayVariant DUEL_SHOP = new PlayVariant(MatchMode.DUEL_PVP, DeckMode.SHOP);

    public boolean isSolo() {
        return matchMode == MatchMode.SOLO_PVE;
    }

    public boolean isDuel() {
        return matchMode == MatchMode.DUEL_PVP;
    }

    public boolean usesShop() {
        return deckMode == DeckMode.SHOP;
    }

    public boolean usesFreeBuild() {
        return deckMode == DeckMode.FREE_BUILD;
    }
}
