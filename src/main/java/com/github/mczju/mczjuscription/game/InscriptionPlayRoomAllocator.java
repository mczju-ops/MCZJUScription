package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.room.AbstractGameRoom;
import com.github.mczjuops.mczjugamecore.game.room.GameRoomState;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * 在插件内占用 / 释放 play 场地，不依赖 GameCore 的 {@code joinGameFresh} 或房间池 API。
 * <p>
 * 仅使用公开的 {@link com.github.mczjuops.mczjugamecore.game.room.GameRoomManager#getGameRoom} 与
 * {@link AbstractGameRoom#setState}。
 */
public final class InscriptionPlayRoomAllocator {

    private InscriptionPlayRoomAllocator() {}

  public static boolean hasLeisureRoom(PlayVariant variant, @Nullable String roomId) {
        return claim(variant, roomId, true) != null;
    }

    /**
     * @param dryRun {@code true} 时只探测不占用
     */
    public static @Nullable InscriptionGameRoom claim(
            PlayVariant variant, @Nullable String roomId, boolean dryRun) {
        Set<String> pool = InscriptionRoomPools.forVariant(variant);
        if (roomId != null && !roomId.isBlank()) {
            String name = roomId.trim();
            if (!pool.contains(name) && !InscriptionRoomPools.HUB.equals(name)) {
                return null;
            }
            return tryClaim(InscriptionGame.GAME_ID, name, dryRun);
        }
        for (String name : pool) {
            InscriptionGameRoom room = tryClaim(InscriptionGame.GAME_ID, name, dryRun);
            if (room != null) {
                return room;
            }
        }
        return null;
    }

    public static void release(@Nullable InscriptionGameRoom room) {
        if (room == null) {
            return;
        }
        if (room.getState() == GameRoomState.IN_GAME) {
            room.setState(GameRoomState.READY);
        }
    }

    private static @Nullable InscriptionGameRoom tryClaim(String gameId, String roomName, boolean dryRun) {
        AbstractGameRoom raw = MCZJUGameCore.getGameRoomManager().getGameRoom(gameId, roomName);
        if (!(raw instanceof InscriptionGameRoom room)) {
            return null;
        }
        if (room.getState() != GameRoomState.READY) {
            return null;
        }
        if (!dryRun) {
            room.setState(GameRoomState.IN_GAME);
        }
        return room;
    }
}
