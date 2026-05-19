package com.github.mczju.mczjuscription.data;

import com.github.mczjuops.mczjugamecore.player.data.JsonPlayerData;

public class InscriptionPlayerData extends JsonPlayerData {
    public Integer wins = 0;
    public Integer gamesPlayed = 0;
    /** 自由构建模式保存的主牌组（模板 ID 列表） */
    public java.util.List<String> savedDeck = new java.util.ArrayList<>();

    /** 排行榜：最高通关难度标量 */
    public Integer bestClearDifficulty = 0;
    /** 在 {@link #bestClearDifficulty} 下的通关次数 */
    public Integer clearCountAtBestDifficulty = 0;

    /** 局外赐福 ID 列表（占位） */
    public java.util.List<String> blessings = new java.util.ArrayList<>();
    /** 所选诅咒 ID：none / glass / ring 等 */
    public String selectedCurse = "none";
}
