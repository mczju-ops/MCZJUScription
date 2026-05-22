package com.github.mczju.mczjuscription.game.wave;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按预定脚本顺序出怪（副本/选关）。
 * <p>
 * 脚本为空时回退到 {@link RandomEnemyWaveSource}。
 */
public final class ScriptEnemyWaveSource implements EnemyWaveSource {

    private final List<EnemyWavePlan> script;
    private final EnemyWaveSource fallback;
    private final List<String> scriptedCreatures = new ArrayList<>();
    private int scriptCreatureCursor;
    private int waveCursor;

    public ScriptEnemyWaveSource(List<EnemyWavePlan> script) {
        this.script = script == null ? List.of() : List.copyOf(script);
        this.fallback = new RandomEnemyWaveSource();
    }

    @Override
    public void onMatchStart(InscriptionMatch match) {
        scriptedCreatures.clear();
        for (EnemyWavePlan plan : script) {
            scriptedCreatures.addAll(plan.templateIds());
        }
        scriptCreatureCursor = 0;
        waveCursor = 0;
        fallback.onMatchStart(match);
    }

    @Override
    public String nextCreature(InscriptionMatch match) {
        if (scriptCreatureCursor < scriptedCreatures.size()) {
            return scriptedCreatures.get(scriptCreatureCursor++);
        }
        return fallback.nextCreature(match);
    }

    @Override
    public EnemyWavePlan nextWave(InscriptionMatch match) {
        if (waveCursor < script.size()) {
            return script.get(waveCursor++);
        }
        return fallback.nextWave(match);
    }

    @Override
    public boolean hasMoreWaves(InscriptionMatch match) {
        return scriptCreatureCursor < scriptedCreatures.size()
                || waveCursor < script.size()
                || fallback.hasMoreWaves(match);
    }

    /** 占位：后续从 room / 关卡 YAML 加载后传入 {@link #ScriptEnemyWaveSource}。 */
    public static EnemyWaveSource forMatch(InscriptionMatch match) {
        List<EnemyWavePlan> loaded = loadFromRoomConfig(match);
        if (loaded.isEmpty()) {
            return new RandomEnemyWaveSource();
        }
        return new ScriptEnemyWaveSource(loaded);
    }

    private static List<EnemyWavePlan> loadFromRoomConfig(InscriptionMatch match) {
        // TODO: 读取 room.enemyWaveScript 或独立关卡配置文件
        return Collections.emptyList();
    }
}
