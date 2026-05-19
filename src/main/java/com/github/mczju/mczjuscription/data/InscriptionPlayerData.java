package com.github.mczju.mczjuscription.data;

import com.github.mczjuops.mczjugamecore.player.data.JsonPlayerData;

public class InscriptionPlayerData extends JsonPlayerData {
    public Integer wins = 0;
    public Integer gamesPlayed = 0;
    /** 自由构建模式保存的主牌组（{@link com.github.mczju.mczjuscription.game.card.CardId} 名称列表） */
    public java.util.List<String> savedDeck = new java.util.ArrayList<>();
}
