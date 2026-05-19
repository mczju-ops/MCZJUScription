package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/**
 * 非玩家一方的回合外逻辑（如 AI 往准备区放牌）。
 * 双人模式下可为 {@code null}，由双方玩家各自操作。
 */
public interface OpponentController {

    MatchSide controlledSide();

    void planPreview(InscriptionMatch match);
}
