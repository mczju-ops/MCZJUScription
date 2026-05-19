package com.github.mczju.mczjuscription.game.session;

/**
 * 对局模式。同一套 {@link com.github.mczju.mczjuscription.game.match.InscriptionMatch} 规则，
 * 通过不同 {@link MatchSetup} 区分单人与双人。
 */
public enum MatchMode {
    /** 一名玩家对战 AI（当前默认） */
    SOLO_PVE,
    /** 两名玩家对战（预留） */
    DUEL_PVP
}
