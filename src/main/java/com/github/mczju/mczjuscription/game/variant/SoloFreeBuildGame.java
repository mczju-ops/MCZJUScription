package com.github.mczju.mczjuscription.game.variant;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.DefaultSinglePlayerGameWaitStrategy;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.GameWaitStrategy;
import org.bukkit.Material;

import java.util.List;

/** 单人 PvE + 自由构建卡组 */
public final class SoloFreeBuildGame extends VariantInscriptionGame {

    public static final String GAME_ID = "inscription";

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public PlayVariant playVariant() {
        return PlayVariant.SOLO_FREE_BUILD;
    }

    @Override
    public GameMeta getGameMeta() {
        return GameMeta.builder()
                .displayName("<dark_gray>邪恶冥刻 · 单人构牌")
                .icon(Material.CANDLE)
                .author("<green>MCZJU")
                .description(List.of(
                        "<gray>请从大厅 <white>inscription_hub</white> 单人座进入",
                        "<gray>调试：可直接 join 本 ID",
                        "<gray>自由构牌 · 敲钟战斗"
                ))
                .build();
    }

    @Override
    public GameWaitStrategy getGameWaitStrategy() {
        return new DefaultSinglePlayerGameWaitStrategy(this);
    }
}
