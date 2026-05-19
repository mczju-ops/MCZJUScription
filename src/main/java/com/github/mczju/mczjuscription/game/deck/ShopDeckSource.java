package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczju.mczjuscription.shop.ShopPresenter;

/** 商店模式：开局空牌组，抽牌阶段可反复打开商店购卡。 */
public final class ShopDeckSource implements DeckSource {

    private final ShopPresenter shopPresenter;

    public ShopDeckSource(ShopPresenter shopPresenter) {
        this.shopPresenter = shopPresenter;
    }

    @Override
    public DeckMode mode() {
        return DeckMode.SHOP;
    }

    @Override
    public void initializeDeck(ParticipantState state, InscriptionMatch match) {
        // 从 0 开始，牌组由购买逐步积累（也可选择购入后直接入手牌）
    }

    @Override
    public void performMainDrawAction(InscriptionMatch match, MatchSide side) {
        shopPresenter.open(match, side);
    }

    @Override
    public String mainDrawItemHint() {
        return "<gray>右键打开商店（可多次购买）";
    }
}
