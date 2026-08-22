package com.github.mczju.mczjuscription.lobby.leaderboard;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.score.leaderboard.LeaderboardEntry;
import com.github.mczjuops.mczjugamecore.serialize.LocationAdapter;
import com.google.gson.Gson;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** 从 inscription 玩家数据聚合通关排行（供 MGC 榜与 GUI 共用）。 */
public final class InscriptionClearRankings {

    public static final int TOP_SIZE = 10;

    public static final double CLEAR_WEIGHT = 1_000_000.0;

    private InscriptionClearRankings() {}

    public static List<LeaderboardEntry> toLeaderboardEntries() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        for (ClearRankRow row : topRows()) {
            // MGC 1.0.8 LeaderboardEntry 只携带 (玩家名, 数值)；数值=sortValue，渲染时拆回难度/次数
            entries.add(new LeaderboardEntry(row.displayName(), row.sortValue()));
        }
        return entries;
    }

    public static List<ClearRankRow> topRows() {
        return loadAll().stream()
                .sorted(Comparator.comparingDouble(ClearRankRow::sortValue).reversed())
                .limit(TOP_SIZE)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<ClearRankRow> loadAll() {
        Map<String, ClearRankRow> best = new HashMap<>();
        Path dir = playerDataDir();
        if (!Files.isDirectory(dir)) {
            return new ArrayList<>();
        }
        Gson gson = LocationAdapter.getGsonBuilder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                String playerId = fileNameToId(file);
                try (FileReader reader = new FileReader(file.toFile(), StandardCharsets.UTF_8)) {
                    InscriptionPlayerData data = gson.fromJson(reader, InscriptionPlayerData.class);
                    if (data != null) {
                        merge(best, playerId, data);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return new ArrayList<>(best.values());
    }

    private static void merge(Map<String, ClearRankRow> best, String playerId, InscriptionPlayerData data) {
        int difficulty = safe(data.bestClearDifficulty);
        int clears = safe(data.clearCountAtBestDifficulty);
        if (difficulty <= 0 && clears <= 0) {
            return;
        }
        ClearRankRow row =
                new ClearRankRow(
                        playerId,
                        displayName(playerId),
                        difficulty,
                        clears,
                        sortValue(difficulty, clears),
                        formatDisplay(difficulty, clears));
        ClearRankRow existing = best.get(playerId);
        if (existing == null || row.sortValue() > existing.sortValue()) {
            best.put(playerId, row);
        }
    }

    static double sortValue(int difficulty, int clears) {
        return difficulty * CLEAR_WEIGHT + clears;
    }

    static String formatDisplay(int difficulty, int clears) {
        return "难度 %d · %d 次".formatted(difficulty, clears);
    }

    private static Path playerDataDir() {
        return Paths.get(
                MCZJUGameCore.getInstance().getDataFolder().getPath(),
                "player",
                AbstractInscriptionGame.DATA_ID);
    }

    private static String fileNameToId(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static int safe(Integer value) {
        return value == null ? 0 : value;
    }

    private static String displayName(String playerId) {
        try {
            UUID uuid = UUID.fromString(playerId);
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            if (offline.getName() != null) {
                return offline.getName();
            }
        } catch (IllegalArgumentException ignored) {
        }
        return playerId.length() > 12 ? playerId.substring(0, 8) + "…" : playerId;
    }

    public record ClearRankRow(
            String playerId,
            String displayName,
            int difficulty,
            int clears,
            double sortValue,
            String displayValue) {}
}
