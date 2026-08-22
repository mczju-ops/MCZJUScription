package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.lobby.leaderboard.InscriptionClearLeaderboard;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;

/** 注册 MGC 排行榜。 */
public final class InscriptionLeaderboards {

    private InscriptionLeaderboards() {}

    public static void registerAll() {
        MCZJUGameCore.getLeaderboardManager()
                .registerLeaderboard(InscriptionClearLeaderboard.ID, InscriptionClearLeaderboard.class);
    }

    public static void refreshClearBoard() {
        MCZJUGameCore.getLeaderboardManager().refresh(InscriptionClearLeaderboard.ID);
    }
}
