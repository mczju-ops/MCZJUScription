package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.match.MatchSide;

/** 射线命中的场地踏板（战斗槽、UI 槽、条带）。 */
public sealed interface ArenaPedalTarget
        permits ArenaPedalTarget.Battle, ArenaPedalTarget.Ui, ArenaPedalTarget.BellStrip, ArenaPedalTarget.PreviewStrip {

    record Battle(SlotOwner owner, int index) implements ArenaPedalTarget {}

    record Ui(MatchSide side, ResolvedArenaLayout.UiSlotKind kind) implements ArenaPedalTarget {}

    /** 敲钟长条（底=己方，顶=对方双人模式）。 */
    record BellStrip(MatchSide side) implements ArenaPedalTarget {}

    /** 单人预览条（仅展示，不可交互）。 */
    record PreviewStrip() implements ArenaPedalTarget {}
}
