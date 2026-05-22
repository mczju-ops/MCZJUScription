package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.lobby.leaderboard.InscriptionClearLeaderboard;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.score.leaderboard.textdisplay.JsonTextDisplayRecord;
import com.github.mczjuops.mczjugamecore.score.leaderboard.textdisplay.TextDisplayRecord;
import org.bukkit.entity.Display;

/** 大厅座位示意 + MGC 通关榜 TextDisplay（位置 {@code leaderboardAt}）。 */
public final class HubDisplayBootstrap {

    public static final String CLEAR_DISPLAY_ID = "hub_main";

    private HubDisplayBootstrap() {}

    public static void ensureHubDecor(InscriptionGameRoom room) {
        if (room == null) {
            return;
        }
        HubSeatMarkerService.ensureSpawned(room);
    }

    /** 同步通关榜展示并确保大厅装饰存在。 */
    public static void sync(InscriptionGameRoom room) {
        if (room == null) {
            return;
        }
        syncClearBoard(room);
        ensureHubDecor(room);
    }

    private static void syncClearBoard(InscriptionGameRoom room) {
        if (room.leaderboardAt == null) {
            return;
        }
        var manager = MCZJUGameCore.getLeaderboardManager();
        TextDisplayRecord record = manager.getDisplayRecord(InscriptionClearLeaderboard.ID, CLEAR_DISPLAY_ID);
        if (record == null) {
            record = new JsonTextDisplayRecord(InscriptionClearLeaderboard.ID, CLEAR_DISPLAY_ID);
            manager.createTextDisplay(InscriptionClearLeaderboard.ID, record);
        }
        record.setLocation(room.leaderboardAt.clone());
        record.getProperties().setBillboard(Display.Billboard.VERTICAL);
        record.getProperties().setHasBackground(false);
        record.setModified(true);
        record.save();
        try {
            manager.refresh(InscriptionClearLeaderboard.ID);
        } catch (Exception ex) {
            MCZJUScriptionPlugin.getInstance()
                    .getLogger()
                    .warning("通关榜刷新失败（不影响进大厅）: " + ex.getMessage());
        }
    }
}
