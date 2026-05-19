package com.github.mczju.mczjuscription.game.turn;

public enum TurnPhase {
    DRAW("抽牌阶段"),
    PLAY("出牌阶段"),
    PLAYER_COMBAT("玩家战斗阶段"),
    ENEMY_PREP("敌人整备阶段"),
    ENEMY_COMBAT("敌人战斗阶段");

    private final String displayName;

    TurnPhase(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public TurnPhase next() {
        TurnPhase[] values = values();
        int next = (ordinal() + 1) % values.length;
        return values[next];
    }
}
