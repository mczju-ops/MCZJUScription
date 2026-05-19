package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.variant.DuelFreeBuildGame;
import com.github.mczju.mczjuscription.game.variant.DuelShopGame;
import com.github.mczju.mczjuscription.game.variant.SoloFreeBuildGame;
import com.github.mczju.mczjuscription.game.variant.SoloShopGame;
import com.github.mczju.mczjuscription.lobby.InscriptionHubGame;
import com.github.mczju.mczjuscription.lobby.InscriptionHubRoom;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;

/**
 * 注册游戏入口。主菜单推荐 {@link InscriptionHubGame}；对局变体由大厅座位启动。
 */
public final class InscriptionGames {

    private InscriptionGames() {}

    public static void registerAll() {
        MCZJUGameCore.getGameManager()
                .registerGame(InscriptionHubGame.class, InscriptionHubRoom.class);
        MCZJUGameCore.getGameManager().registerGame(SoloFreeBuildGame.class, InscriptionGameRoom.class);
        MCZJUGameCore.getGameManager().registerGame(SoloShopGame.class, InscriptionGameRoom.class);
        registerDuelVariants();
    }

    /** 注册双人 PvP 变体（大厅双人座需要）。 */
    public static void registerDuelVariants() {
        MCZJUGameCore.getGameManager().registerGame(DuelFreeBuildGame.class, InscriptionGameRoom.class);
        MCZJUGameCore.getGameManager().registerGame(DuelShopGame.class, InscriptionGameRoom.class);
    }
}
