package com.github.mczju.mczjuscription.game.match;

/**
 * 棋盘上的两个对阵方（座位 0 / 座位 1）。
 * <ul>
 *   <li>单人：{@link #PLAYER} = 玩家，{@link #ENEMY} = AI</li>
 *   <li>双人：{@link #PLAYER} = 先手玩家，{@link #ENEMY} = 后手玩家</li>
 * </ul>
 */
public enum MatchSide {
    PLAYER,
    ENEMY;

    public MatchSide opposite() {
        return this == PLAYER ? ENEMY : PLAYER;
    }
}
