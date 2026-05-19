package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczjuops.mczjugamecore.game.room.JsonGameRoom;
import org.bukkit.Location;

/**
 * 房间参数。槽位坐标可通过 /mgcop room edit 配置；
 * 若未配置，对局中仍可用 /isc arena 临时生成场地。
 */
public class InscriptionGameRoom extends JsonGameRoom {
    public Location spawnAt;
    public Integer playerCandles;
    public Integer enemyCandles;
    /** 敲钟交互点（与槽位相同：房间编辑里标定的中心坐标）。 */
    public Location clockAt;

    /** 流浪商人站位（第 3、6、9… 回合出现）。 */
    public Location traderAt;
    /** 商人朝向（度），未设则用 {@code traderAt} 自带 yaw。 */
    public Float traderYaw;

    public Location playerSlot0;
    public Location playerSlot1;
    public Location playerSlot2;
    public Location playerSlot3;

    public Location enemySlot0;
    public Location enemySlot1;
    public Location enemySlot2;
    public Location enemySlot3;

    public Location previewSlot0;
    public Location previewSlot1;
    public Location previewSlot2;
    public Location previewSlot3;

    public boolean hasConfiguredArena() {
        return playerSlot0 != null && enemySlot0 != null && previewSlot0 != null;
    }

    public Location slotLocation(SlotOwner owner, int index) {
        return switch (owner) {
            case PLAYER -> playerSlot(index);
            case ENEMY -> enemySlot(index);
            case ENEMY_PREVIEW -> previewSlot(index);
        };
    }

    private Location playerSlot(int index) {
        return switch (index) {
            case 0 -> playerSlot0;
            case 1 -> playerSlot1;
            case 2 -> playerSlot2;
            case 3 -> playerSlot3;
            default -> null;
        };
    }

    private Location enemySlot(int index) {
        return switch (index) {
            case 0 -> enemySlot0;
            case 1 -> enemySlot1;
            case 2 -> enemySlot2;
            case 3 -> enemySlot3;
            default -> null;
        };
    }

    private Location previewSlot(int index) {
        return switch (index) {
            case 0 -> previewSlot0;
            case 1 -> previewSlot1;
            case 2 -> previewSlot2;
            case 3 -> previewSlot3;
            default -> null;
        };
    }
}
