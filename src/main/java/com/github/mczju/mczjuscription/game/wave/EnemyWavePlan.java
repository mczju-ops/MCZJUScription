package com.github.mczju.mczjuscription.game.wave;

import java.util.List;

/** 一批将进入敌方后场的造物（配置/脚本用）。 */
public record EnemyWavePlan(List<String> templateIds) {

    public EnemyWavePlan {
        templateIds = templateIds == null ? List.of() : List.copyOf(templateIds);
    }

    public static EnemyWavePlan of(String... templateIds) {
        return new EnemyWavePlan(List.of(templateIds));
    }
}
