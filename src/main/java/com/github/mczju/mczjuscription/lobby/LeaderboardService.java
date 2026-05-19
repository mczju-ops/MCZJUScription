package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** 排行榜：按通关难度、同难度按通关次数。 */
public final class LeaderboardService {

    public static final int TOP_SIZE = 10;

    private LeaderboardService() {}

    public static List<LeaderboardEntry> topEntries() {
        List<LeaderboardEntry> all = loadAll();
        all.sort(
                Comparator.comparingInt(LeaderboardEntry::difficulty)
                        .reversed()
                        .thenComparingInt(LeaderboardEntry::clears)
                        .reversed());
        if (all.size() <= TOP_SIZE) {
            return all;
        }
        return all.subList(0, TOP_SIZE);
    }

    private static List<LeaderboardEntry> loadAll() {
        List<LeaderboardEntry> entries = new ArrayList<>();
        entries.addAll(fromMemory());
        entries.addAll(fromDisk());
        return mergeByPlayer(entries);
    }

    private static List<LeaderboardEntry> fromMemory() {
        List<LeaderboardEntry> list = new ArrayList<>();
        try {
            var manager = MCZJUGameCore.getPlayerDataManager();
            var field =
                    manager.getClass().getDeclaredField("pDataMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Map<String, ?>> map =
                    (Map<String, Map<String, ?>>) field.get(manager);
            Map<String, ?> gameMap = map.get(AbstractInscriptionGame.DATA_ID);
            if (gameMap == null) {
                return list;
            }
            for (var entry : gameMap.entrySet()) {
                if (entry.getValue() instanceof InscriptionPlayerData data) {
                    addEntry(list, entry.getKey(), data);
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // 仅使用磁盘数据
        }
        return list;
    }

    private static List<LeaderboardEntry> fromDisk() {
        List<LeaderboardEntry> list = new ArrayList<>();
        Path dir =
                Paths.get(
                        MCZJUGameCore.getInstance().getDataFolder().getPath(),
                        "player",
                        AbstractInscriptionGame.DATA_ID);
        if (!Files.isDirectory(dir)) {
            return list;
        }
        Gson gson = LocationAdapter.getGsonBuilder();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                int dot = name.lastIndexOf('.');
                String playerId = dot > 0 ? name.substring(0, dot) : name;
                try (FileReader reader = new FileReader(file.toFile(), StandardCharsets.UTF_8)) {
                    InscriptionPlayerData data = gson.fromJson(reader, InscriptionPlayerData.class);
                    if (data != null) {
                        addEntry(list, playerId, data);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private static void addEntry(List<LeaderboardEntry> list, String playerId, InscriptionPlayerData data) {
        int difficulty = safe(data.bestClearDifficulty);
        int clears = safe(data.clearCountAtBestDifficulty);
        if (difficulty <= 0 && clears <= 0) {
            return;
        }
        list.add(new LeaderboardEntry(playerId, displayName(playerId), difficulty, clears));
    }

    private static List<LeaderboardEntry> mergeByPlayer(List<LeaderboardEntry> raw) {
        Map<String, LeaderboardEntry> best = new java.util.HashMap<>();
        for (LeaderboardEntry entry : raw) {
            LeaderboardEntry existing = best.get(entry.playerId());
            if (existing == null || entry.compareTo(existing) > 0) {
                best.put(entry.playerId(), entry);
            }
        }
        return new ArrayList<>(best.values());
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

    public record LeaderboardEntry(String playerId, String displayName, int difficulty, int clears)
            implements Comparable<LeaderboardEntry> {

        @Override
        public int compareTo(LeaderboardEntry other) {
            int d = Integer.compare(difficulty, other.difficulty);
            if (d != 0) {
                return d;
            }
            return Integer.compare(clears, other.clears);
        }
    }
}
