package com.github.mczju.mczjuscription.game.variant;

import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczju.mczjuscription.game.session.MatchSetup;
import com.github.mczju.mczjuscription.game.session.MatchSetupFactory;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczju.mczjuscription.player.InscriptionPlayerQuitStrategy;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.strategy.AbstractPlayerQuitStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 按 {@link PlayVariant} 区分的游戏实现基类。
 * 子类仅需声明变体、GameCore 等待策略与展示元数据。
 */
public abstract class VariantInscriptionGame extends AbstractInscriptionGame {

    public abstract PlayVariant playVariant();

    @Override
    public final MatchMode getMatchMode() {
        return playVariant().matchMode();
    }

    public final DeckMode getDeckMode() {
        return playVariant().deckMode();
    }

    @Override
    protected final MatchSetup buildMatchSetup(List<PlayerExt> players, InscriptionGameRoom room) {
        return MatchSetupFactory.create(playVariant(), players, room);
    }

    @Override
    public @NotNull AbstractPlayerQuitStrategy getPlayerQuitStrategy() {
        return new InscriptionPlayerQuitStrategy(this);
    }
}
