package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.game.match.MatchSide;

public enum SlotOwner {
    PLAYER(MatchSide.PLAYER),
    ENEMY(MatchSide.ENEMY),
    ENEMY_PREVIEW(MatchSide.ENEMY);

    private final MatchSide side;

    SlotOwner(MatchSide side) {
        this.side = side;
    }

    public MatchSide side() {
        return side;
    }
}
