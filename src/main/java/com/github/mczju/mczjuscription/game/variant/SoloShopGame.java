package com.github.mczju.mczjuscription.game.variant;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.DefaultSinglePlayerGameWaitStrategy;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.GameWaitStrategy;
import org.bukkit.Material;

import java.util.List;

/** 单人 PvE + 商店构筑 */
public final class SoloShopGame extends VariantInscriptionGame {

    public static final String GAME_ID = "inscription_solo_shop";

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public PlayVariant playVariant() {
        return PlayVariant.SOLO_SHOP;
    }

    @Override
    public GameMeta getGameMeta() {
        return GameMeta.builder()
                .displayName("<dark_red>邪恶冥刻 <gold>(商店)")
                .icon(Material.EMERALD)
                .author("<green>MCZJU")
                .description(List.of(
                        "<gray>单人 PvE · 推荐试玩",
                        "<gold>每回合商店购卡（开局 3 骨币）",
                        "<gray>献祭获腐肉，敲钟进入战斗"
                ))
                .build();
    }

    @Override
    public GameWaitStrategy getGameWaitStrategy() {
        return new DefaultSinglePlayerGameWaitStrategy(this);
    }
}
