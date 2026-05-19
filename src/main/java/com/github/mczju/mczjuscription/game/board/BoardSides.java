package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.game.match.MatchSide;

/** {@link MatchSide} 与棋盘 {@link SlotOwner} 的映射。 */
public final class BoardSides {

    private BoardSides() {}

    public static SlotOwner toSlotOwner(MatchSide side) {
        return side == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
    }

    public static MatchSide toMatchSide(SlotOwner owner) {
        if (owner == SlotOwner.ENEMY_PREVIEW) {
            return MatchSide.ENEMY;
        }
        return owner == SlotOwner.PLAYER ? MatchSide.PLAYER : MatchSide.ENEMY;
    }
}
