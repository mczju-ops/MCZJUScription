package com.github.mczju.mczjuscription.game.ai;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.OpponentController;

/** 单人 PvE 的 AI 实现。 */
public final class AiOpponentController implements OpponentController {

    private EnemyPlanner planner;

    @Override
    public MatchSide controlledSide() {
        return MatchSide.ENEMY;
    }

    @Override
    public void planPreview(InscriptionMatch match) {
        if (planner == null) {
            planner = new EnemyPlanner(match);
        }
        planner.planNextPreview();
    }
}
