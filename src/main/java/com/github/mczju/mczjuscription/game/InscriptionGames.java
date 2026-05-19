package com.github.mczju.mczjuscription.game;

import com.github.mczjuops.mczjugamecore.MCZJUGameCore;

/** 向 MGC 注册邪恶冥刻（唯一 gameId，四种模式为参数而非独立游戏）。 */
public final class InscriptionGames {

    private InscriptionGames() {}

    public static void registerAll() {
        MCZJUGameCore.getGameManager()
                .registerGame(InscriptionGame.class, InscriptionGameRoom.class);
    }
}
