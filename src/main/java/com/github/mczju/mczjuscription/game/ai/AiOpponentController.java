package com.github.mczju.mczjuscription.game.ai;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.OpponentController;
import com.github.mczju.mczjuscription.game.wave.EnemyWaveDirector;
import com.github.mczju.mczjuscription.game.wave.ScriptEnemyWaveSource;
import java.util.List;

/** 单人 PvE 的 AI 实现。 */
public final class AiOpponentController implements OpponentController {

    private EnemyWaveDirector waveDirector;

    private EnemyWaveDirector director(InscriptionMatch match) {
        if (waveDirector == null) {
            waveDirector = new EnemyWaveDirector(ScriptEnemyWaveSource.forMatch(match));
        }
        return waveDirector;
    }

    @Override
    public MatchSide controlledSide() {
        return MatchSide.ENEMY;
    }

    @Override
    public void onMatchStart(InscriptionMatch match) {
        director(match).onMatchStart(match);
    }

    @Override
    public void planPreview(InscriptionMatch match) {
        director(match).deployOneToBackfield(match);
    }

    @Override
    public void onBackfieldWaveAdvanced(InscriptionMatch match) {
        director(match).onPipelineStep(match);
    }

    @Override
    public void onRoundReset(InscriptionMatch match) {
        director(match).onRoundReset(match);
    }

    @Override
    public List<String> stripPreviewTemplates(InscriptionMatch match) {
        return director(match).stripPreviewTemplates();
    }
}
