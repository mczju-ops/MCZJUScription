package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import org.jetbrains.annotations.Nullable;

/**
 * 对局中的一方：可绑定真实玩家，或由 AI 控制。
 */
public final class MatchParticipant {

    private final MatchSide side;
    private final @Nullable PlayerExt player;
    private final boolean aiControlled;

    private MatchParticipant(MatchSide side, @Nullable PlayerExt player, boolean aiControlled) {
        this.side = side;
        this.player = player;
        this.aiControlled = aiControlled;
    }

    public static MatchParticipant human(MatchSide side, PlayerExt player) {
        return new MatchParticipant(side, player, false);
    }

    public static MatchParticipant ai(MatchSide side) {
        return new MatchParticipant(side, null, true);
    }

    public MatchSide side() {
        return side;
    }

    public @Nullable PlayerExt player() {
        return player;
    }

    public boolean isAiControlled() {
        return aiControlled;
    }

    public boolean isHuman() {
        return player != null && !aiControlled;
    }
}
