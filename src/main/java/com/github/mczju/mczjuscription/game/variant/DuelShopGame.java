package com.github.mczju.mczjuscription.game.variant;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.DefaultGameWaitStrategy;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.GameWaitStrategy;
import org.bukkit.Material;

import java.util.List;

/** 双人 PvP + 商店构筑（预留） */
public final class DuelShopGame extends VariantInscriptionGame {

    public static final String GAME_ID = "inscription_duel_shop";

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public PlayVariant playVariant() {
        return PlayVariant.DUEL_SHOP;
    }

    @Override
    public GameMeta getGameMeta() {
        return GameMeta.builder()
                .displayName("<dark_red>邪恶冥刻 <aqua>(双人·商店)")
                .icon(Material.EMERALD)
                .author("<green>MCZJU")
                .description(List.of(
                        "<gray>双人对战",
                        "<gold>每回合商店购卡",
                        "<dark_red>战斗流程开发中"
                ))
                .build();
    }

    @Override
    public GameWaitStrategy getGameWaitStrategy() {
        return new DefaultGameWaitStrategy(this, 2, 2);
    }
}
