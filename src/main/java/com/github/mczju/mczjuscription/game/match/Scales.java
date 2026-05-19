package com.github.mczju.mczjuscription.game.match;

/**
 * 天平净胜分：玩家受伤向右（负），敌人受伤向左（正）。
 * 达到 ±{@link #WIN_THRESHOLD} 时小局结束。
 */
public final class Scales {

    public static final int WIN_THRESHOLD = 5;
    public static final int MIN = -WIN_THRESHOLD;
    public static final int MAX = WIN_THRESHOLD;

    private int value;

    public int getValue() {
        return value;
    }

    /** 玩家本体受伤：天平向负方向偏移 */
    public void damagePlayer(int amount) {
        value = clamp(value - amount);
    }

    /** 敌人本体受伤：天平向正方向偏移 */
    public void damageEnemy(int amount) {
        value = clamp(value + amount);
    }

    public MatchSide checkRoundWinner() {
        if (value >= WIN_THRESHOLD) return MatchSide.PLAYER;
        if (value <= -WIN_THRESHOLD) return MatchSide.ENEMY;
        return null;
    }

    public void reset() {
        value = 0;
    }

    /**
     * BossBar 进度：0 = 我方被打满（{@link #MIN}），1 = 敌方被打满（{@link #MAX}）。
     */
    public float bossBarProgress() {
        return (value - MIN) / (float) (MAX - MIN);
    }

    private static int clamp(int v) {
        return Math.max(MIN, Math.min(MAX, v));
    }
}
