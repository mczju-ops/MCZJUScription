package com.github.mczju.mczjuscription.game.wave;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

/**
 * 敌方出怪来源。
 * <p>
 * 流水线模式每次 {@link #nextCreature} 抽取一只；{@link #nextWave} 保留给 YAML 脚本批量定义。
 */
public interface EnemyWaveSource {

    /** 对局开始时重置游标。 */
    void onMatchStart(InscriptionMatch match);

    /** 抽取下一只将进入流水线的造物模板 id。 */
    String nextCreature(InscriptionMatch match);

    /** 取下一波（脚本批量用）。无更多波次时返回空列表。 */
    EnemyWavePlan nextWave(InscriptionMatch match);

    /** 是否还有未读取的波次/卡牌。 */
    default boolean hasMoreWaves(InscriptionMatch match) {
        return true;
    }
}
