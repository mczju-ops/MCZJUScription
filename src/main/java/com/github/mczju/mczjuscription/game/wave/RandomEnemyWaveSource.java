package com.github.mczju.mczjuscription.game.wave;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 单人默认：每次随机抽 1 只造物。 */
public final class RandomEnemyWaveSource implements EnemyWaveSource {

    @Override
    public void onMatchStart(InscriptionMatch match) {}

    @Override
    public String nextCreature(InscriptionMatch match) {
        return pickCard();
    }

    @Override
    public EnemyWavePlan nextWave(InscriptionMatch match) {
        return EnemyWavePlan.of(nextCreature(match));
    }

    private static String pickCard() {
        List<String> pool = CardCatalog.deckBuilderPool();
        if (pool.isEmpty()) {
            return "mob_rabbit";
        }
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
