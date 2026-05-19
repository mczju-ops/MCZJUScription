package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.match.MatchSide;

/**
 * 当前回合由哪一方行动（出牌、献祭等）。
 * 单人模式固定为 {@link MatchSide#PLAYER}；双人模式将来可交替。
 */
public final class TurnOwnership {

    private final MatchMode mode;
    private MatchSide activeSide = MatchSide.PLAYER;

    public TurnOwnership(MatchMode mode) {
        this.mode = mode;
    }

    public MatchMode matchMode() {
        return mode;
    }

    public MatchSide activeSide() {
        return activeSide;
    }

    public void setActiveSide(MatchSide side) {
        this.activeSide = side;
    }

    /** 单人 PvE：始终由玩家方操作；双人：返回当前 activeSide。 */
    public MatchSide actingSideForInput() {
        if (mode == MatchMode.SOLO_PVE) {
            return MatchSide.PLAYER;
        }
        return activeSide;
    }

    public void endTurn() {
        if (mode == MatchMode.DUEL_PVP) {
            activeSide = activeSide.opposite();
        }
    }
}
