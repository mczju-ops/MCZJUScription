package com.github.mczju.mczjuscription.game.session;

/**
 * 卡组来源模式。
 * <ul>
 *   <li>{@link #FREE_BUILD} — 开局携带自建主牌组，抽牌阶段从牌组顶抽取</li>
 *   <li>{@link #SHOP} — 开局空牌组，抽牌阶段打开商店购买卡牌入手牌</li>
 * </ul>
 */
public enum DeckMode {
    FREE_BUILD,
    SHOP
}
