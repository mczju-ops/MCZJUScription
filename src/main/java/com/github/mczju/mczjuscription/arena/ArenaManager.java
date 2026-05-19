package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaManager {

    private static final Map<UUID, BattleArena> ARENAS = new ConcurrentHashMap<>();
    private static BattleArena sharedRoomArena;

    private ArenaManager() {}

    public static BattleArena createAtPlayer(Player player) {
        remove(player.getUniqueId());
        BattleArena arena = BattleArena.atPlayer(player);
        ARENAS.put(player.getUniqueId(), arena);
        return arena;
    }

    public static void bindSharedRoomArena(BattleArena arena) {
        if (sharedRoomArena != null) {
            sharedRoomArena.destroy();
        }
        sharedRoomArena = arena;
    }

    public static BattleArena sharedRoomArena() {
        return sharedRoomArena;
    }

    public static BattleArena get(Player player) {
        return ARENAS.get(player.getUniqueId());
    }

    public static void remove(UUID playerId) {
        BattleArena arena = ARENAS.remove(playerId);
        if (arena != null) arena.destroy();
    }

    public static void cleanupAll() {
        ARENAS.values().forEach(BattleArena::destroy);
        ARENAS.clear();
        if (sharedRoomArena != null) {
            sharedRoomArena.destroy();
            sharedRoomArena = null;
        }
    }
}
