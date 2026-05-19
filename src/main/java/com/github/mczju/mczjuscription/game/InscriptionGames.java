package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.variant.DuelFreeBuildGame;
import com.github.mczju.mczjuscription.game.variant.DuelShopGame;
import com.github.mczju.mczjuscription.game.variant.SoloFreeBuildGame;
import com.github.mczju.mczjuscription.game.variant.SoloShopGame;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;

/**
 * 注册游戏入口。当前阶段仅开放单人玩法（自由构牌 + 商店）。
 * 双人变体实现保留在代码中，待 PvP 战斗流程完成后再注册。
 */
public final class InscriptionGames {

    private InscriptionGames() {}

    public static void registerAll() {
        MCZJUGameCore.getGameManager().registerGame(SoloFreeBuildGame.class, InscriptionGameRoom.class);
        MCZJUGameCore.getGameManager().registerGame(SoloShopGame.class, InscriptionGameRoom.class);
    }

    /** 双人模式完成后调用，注册 PvP 入口。 */
    public static void registerDuelVariants() {
        MCZJUGameCore.getGameManager().registerGame(DuelFreeBuildGame.class, InscriptionGameRoom.class);
        MCZJUGameCore.getGameManager().registerGame(DuelShopGame.class, InscriptionGameRoom.class);
    }
}
