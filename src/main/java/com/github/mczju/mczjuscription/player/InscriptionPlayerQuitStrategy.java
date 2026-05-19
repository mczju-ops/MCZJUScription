package com.github.mczju.mczjuscription.player;

import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczjuops.mczjugamecore.game.AbstractGame;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.strategy.AbstractPlayerQuitStrategy;
import com.github.mczjuops.mczjugamecore.player.strategy.PlayerQuitReason;

/** 离开对局时清理背包中的局内物品，并处理胜负。 */
public final class InscriptionPlayerQuitStrategy extends AbstractPlayerQuitStrategy {

    public InscriptionPlayerQuitStrategy(AbstractGame game) {
        super(game);
    }

    @Override
    public void onPlayerQuit(PlayerExt player, PlayerQuitReason reason) {
        InscriptionItems.stripPlayerInventory(player.player());

        if (!(game instanceof AbstractInscriptionGame inscriptionGame)) {
            return;
        }
        InscriptionMatch match = inscriptionGame.match();
        if (match == null || match.isMatchOver()) {
            return;
        }
        if (inscriptionGame.getState() != GameState.RUNNING) {
            return;
        }

        MatchSide side = match.sideFor(player.player());
        if (side == null) {
            return;
        }

        MatchMode mode = inscriptionGame.getMatchMode();
        if (mode == MatchMode.DUEL_PVP || mode == MatchMode.SOLO_PVE) {
            match.forfeit(side);
            if (mode == MatchMode.SOLO_PVE) {
                player.sender().warn("<red>你已退出对局，判负。");
            }
        }
    }
}
