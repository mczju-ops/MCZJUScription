package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.ParticipantState;

/**
 * 某一方的卡组/抽牌逻辑。与 {@link DeckMode} 一一对应实现。
 */
public interface DeckSource {

    DeckMode mode();

    /** 对局开始时初始化主牌组（商店模式通常保持为空）。 */
    void initializeDeck(ParticipantState state, InscriptionMatch match);

    /**
     * 抽牌阶段：右键「主牌组/商店」道具时调用。
     * @return 是否已处理（处理成功后由实现方调用 {@link InscriptionMatch#completeMainDraw(MatchSide)}）
     */
    void performMainDrawAction(InscriptionMatch match, MatchSide side);

    /** 主牌组道具在物品 Lore 中的说明 */
    String mainDrawItemHint();
}
