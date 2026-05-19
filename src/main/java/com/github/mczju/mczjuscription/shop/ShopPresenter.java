package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/**
 * 商店 UI 入口。可实现为 Chest GUI、Dialog 等。
 */
public interface ShopPresenter {

    void open(InscriptionMatch match, MatchSide buyerSide);

    /**
     * 玩家完成购买后由 UI 调用。
     * @return 是否购买成功并完成本回合抽牌
     */
    boolean purchase(InscriptionMatch match, MatchSide buyerSide, CardId cardId);
}
