package com.github.mczju.mczjuscription.game.variant;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.DefaultGameWaitStrategy;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.GameWaitStrategy;
import org.bukkit.Material;

import java.util.List;

/** 双人 PvP + 自由构建（预留，战斗流程待完善） */
public final class DuelFreeBuildGame extends VariantInscriptionGame {

    public static final String GAME_ID = "inscription_duel_build";

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public PlayVariant playVariant() {
        return PlayVariant.DUEL_FREE_BUILD;
    }

    @Override
    public GameMeta getGameMeta() {
        return GameMeta.builder()
                .displayName("<dark_red>邪恶冥刻 <aqua>(双人·构牌)")
                .icon(Material.CANDLE)
                .author("<green>MCZJU")
                .description(List.of(
                        "<gray>双人对战",
                        "<gray>自由构建卡组",
                        "<dark_red>战斗流程开发中"
                ))
                .build();
    }

    @Override
    public GameWaitStrategy getGameWaitStrategy() {
        return new DefaultGameWaitStrategy(this, 2, 2);
    }
}
