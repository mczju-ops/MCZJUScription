package com.github.mczju.mczjuscription.lobby;

import com.github.mczjuops.mczjugamecore.game.room.JsonGameRoom;
import org.bukkit.Location;

/** 邪恶冥刻等待大厅房间配置（{@code plugins/MCZJUGameCore/rooms/inscription_hub/*.json}）。 */
public class InscriptionHubRoom extends JsonGameRoom {

    public Location spawnAt;

    public Location deckLecternAt;
    public Location blessingAt;
    public Location curseSelectAt;
    public Location leaderboardAt;

    public Location soloSeat0;
    public Location soloSeat1;
    public Location soloSeat2;
    public Location soloSeat3;

    public Location duelSeat0;
    public Location duelSeat1;
    public Location duelSeat2;
    public Location duelSeat3;

    public Location signAt0;
    public Location signAt1;
    public Location signAt2;
    public Location signAt3;
    public Location signAt4;
    public Location signAt5;
    public Location signAt6;
    public Location signAt7;

    public String signText0;
    public String signText1;
    public String signText2;
    public String signText3;
    public String signText4;
    public String signText5;
    public String signText6;
    public String signText7;
}
