package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import java.util.Collections;
import java.util.List;

/**
 * 非玩家一方的回合外逻辑（如 AI 往准备区放牌）。
 * 双人模式下可为 {@code null}，由双方玩家各自操作。
 */
public interface OpponentController {

    MatchSide controlledSide();

    /** 对局开始时初始化出怪波次等。 */
    default void onMatchStart(InscriptionMatch match) {}

    /** 回合结束时往后场区补充造物。 */
    void planPreview(InscriptionMatch match);

    /** 后场区造物进入站场每推进一步（顶栏→后场→站场流水线）。 */
    default void onBackfieldWaveAdvanced(InscriptionMatch match) {}

    /** 小局蜡烛结算清场后，恢复出怪流水线。 */
    default void onRoundReset(InscriptionMatch match) {}

    /**
     * 顶栏 2×11 预览：下一只将进入后场的造物模板 id。
     * 单人 PvE 用于预渲染图标；默认无预览。
     */
    default List<String> stripPreviewTemplates(InscriptionMatch match) {
        return Collections.emptyList();
    }
}
