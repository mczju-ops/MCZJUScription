package com.github.mczju.mczjuscription.game.match;

/**
 * 蜡烛：一方蜡烛耗尽则整局结束。
 */
public final class LifeSystem {

    public static final int DEFAULT_PLAYER_CANDLES = 2;
    public static final int DEFAULT_ENEMY_CANDLES = 3;

    private int playerCandles = DEFAULT_PLAYER_CANDLES;
    private int enemyCandles = DEFAULT_ENEMY_CANDLES;

    public int getCandles(MatchSide side) {
        return side == MatchSide.PLAYER ? playerCandles : enemyCandles;
    }

    public boolean isDefeated(MatchSide side) {
        return getCandles(side) <= 0;
    }

    public void extinguish(MatchSide loser) {
        if (loser == MatchSide.PLAYER) {
            playerCandles = Math.max(0, playerCandles - 1);
        } else {
            enemyCandles = Math.max(0, enemyCandles - 1);
        }
    }

    public void resetForNewMatch(int playerCandles, int enemyCandles) {
        this.playerCandles = playerCandles;
        this.enemyCandles = enemyCandles;
    }
}
