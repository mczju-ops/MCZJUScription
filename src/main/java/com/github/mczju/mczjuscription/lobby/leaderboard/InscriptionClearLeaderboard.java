package com.github.mczju.mczjuscription.lobby.leaderboard;

import com.github.mczjuops.mczjugamecore.score.leaderboard.AbstractLeaderboard;
import com.github.mczjuops.mczjugamecore.score.leaderboard.LeaderboardEntry;
import com.github.mczjuops.mczjugamecore.score.leaderboard.SortOrder;
import com.github.mczjuops.mczjugamecore.utils.TextParser;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

/** 邪恶冥刻通关榜：难度优先，同难度比通关次数（MGC {@link AbstractLeaderboard}）。 */
public final class InscriptionClearLeaderboard extends AbstractLeaderboard {

    public static final String ID = "inscription_clear";

    @Override
    public String getTitle() {
        return "<gold><b>通关排行榜";
    }

    @Override
    public String getSubtitle() {
        return "<gray>前十名 · 难度优先";
    }

    @Override
    public List<LeaderboardEntry> fetchEntries() {
        try {
            return new ArrayList<>(InscriptionClearRankings.toLeaderboardEntries());
        } catch (Throwable ex) {
            return new ArrayList<>();
        }
    }

    @Override
    public SortOrder getSortOrder() {
        return SortOrder.DESCENDING;
    }

    @Override
    public int getDisplayCount() {
        return InscriptionClearRankings.TOP_SIZE;
    }

    @Override
    public boolean autoRefresh() {
        // 由大厅进入 / 右键榜 / 对局结束时主动 refresh，避免 MGC 定时任务在 /reload 后踩已关闭的插件 ClassLoader
        return false;
    }

    @Override
    public Component renderLine(int rank, String playerName, String displayValue) {
        return TextParser.parse(
                "<yellow>%d.</yellow> <white>%s</white> <gray>—</gray> <aqua>%s"
                        .formatted(rank, playerName, displayValue));
    }
}
