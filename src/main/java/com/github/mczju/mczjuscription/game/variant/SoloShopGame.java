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
                .displayName("<dark_gray>邪恶冥刻 · 单人商店")
                .icon(Material.EMERALD)
                .author("<green>MCZJU")
                .description(List.of(
                        "<gray>请从大厅单人座进入",
                        "<gold>每回合商店购卡",
                        "<gray>调试：可直接 join"
                ))
                .build();
    }

    @Override
    public GameWaitStrategy getGameWaitStrategy() {
        return new DefaultSinglePlayerGameWaitStrategy(this);
    }
}
