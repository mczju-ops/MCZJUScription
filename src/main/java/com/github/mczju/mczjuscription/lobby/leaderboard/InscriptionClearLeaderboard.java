package com.github.mczju.mczjuscription.lobby.leaderboard;

import com.github.mczjuops.mczjugamecore.score.leaderboard.AbstractLeaderboard;
import com.github.mczjuops.mczjugamecore.score.leaderboard.LeaderboardEntry;
import com.github.mczjuops.mczjugamecore.score.leaderboard.SortOrder;
import java.util.ArrayList;
import java.util.List;

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
    public String renderLine(int rank, String playerName, double value) {
        // 值 = 难度 * CLEAR_WEIGHT + 通关次数（与 InscriptionClearRankings.sortValue 一致），在此拆回展示
        int difficulty = (int) (value / InscriptionClearRankings.CLEAR_WEIGHT);
        int clears = (int) (value % InscriptionClearRankings.CLEAR_WEIGHT);
        return "<yellow>%d.</yellow> <white>%s</white> <gray>—</gray> <aqua>难度 %d · %d 次"
                .formatted(rank, playerName, difficulty, clears);
    }
}
