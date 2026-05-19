package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.menu.InscriptionShopMenu;

/**
 * 简易商店：抽牌阶段可反复打开，每次购买一张卡（骨币支付）。
 */
public final class SimpleShopPresenter implements ShopPresenter {

    @Override
    public void open(InscriptionMatch match, MatchSide buyerSide) {
        if (!match.turn().canDrawFromMainDeck()) {
            return;
        }
        match.participant(buyerSide).player().ifPresent(ext ->
                new InscriptionShopMenu(ext.player(), match, buyerSide, this).open()
        );
    }

    @Override
    public boolean purchase(InscriptionMatch match, MatchSide buyerSide, CardId cardId) {
        int price = ShopCatalog.bonePrice(cardId);
        var currency = match.currency(buyerSide);
        if (price > 0 && !currency.trySpendBones(price)) {
            match.feedback().actionBarWarn("<red>骨币不足 ×%d".formatted(price));
            return false;
        }
        match.grantCardToHandSilent(buyerSide, cardId.name());
        match.participant(buyerSide).mainDeck().addLast(cardId.name());
        match.completeShopPurchase(buyerSide);
        return true;
    }
}
