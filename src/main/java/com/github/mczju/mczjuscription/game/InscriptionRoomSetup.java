package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/** 启动时检查房间 JSON 是否齐全。 */
public final class InscriptionRoomSetup {

    private InscriptionRoomSetup() {}

    public static void validateAndLog(JavaPlugin plugin) {
        Set<String> registered =
                MCZJUGameCore.getGameRoomManager().getGameRoomNames(InscriptionGame.GAME_ID);
        List<String> missing = new ArrayList<>();
        check(missing, registered, InscriptionRoomPools.HUB);
        InscriptionRoomPools.SOLO.forEach(name -> check(missing, registered, name));
        InscriptionRoomPools.DUEL.forEach(name -> check(missing, registered, name));
        if (missing.isEmpty()) {
            plugin.getLogger()
                    .info(
                            "房间池就绪：大厅 %s，单人 play10～19，双人 play20～24（共 %d 个场地）"
                                    .formatted(
                                            InscriptionRoomPools.HUB,
                                            InscriptionRoomPools.SOLO.size()
                                                    + InscriptionRoomPools.DUEL.size()));
            return;
        }
        plugin.getLogger()
                .warning(
                        "缺少 inscription 房间 JSON（/mgcop room create inscription <名>）："
                                + String.join(", ", missing));
    }

    public static boolean hasLeisureRoom(Set<String> pool) {
        return hasLeisureRoom(pool, null);
    }

    public static boolean hasLeisureRoom(Set<String> pool, @Nullable String roomId) {
        if (pool.equals(InscriptionRoomPools.HUB_ONLY)) {
            String room = roomId != null && !roomId.isBlank() ? roomId.trim() : InscriptionRoomPools.HUB;
            return MCZJUGameCore.getGameRoomManager().getGameRoom(InscriptionGame.GAME_ID, room) != null;
        }
        PlayVariant variant = variantForPool(pool);
        return variant != null && InscriptionPlayRoomAllocator.hasLeisureRoom(variant, roomId);
    }

    private static @Nullable PlayVariant variantForPool(Set<String> pool) {
        if (pool.equals(InscriptionRoomPools.SOLO)) {
            return PlayVariant.SOLO_SHOP;
        }
        if (pool.equals(InscriptionRoomPools.DUEL)) {
            return PlayVariant.DUEL_SHOP;
        }
        return null;
    }

    private static void check(List<String> missing, Set<String> registered, String name) {
        if (!registered.contains(name)) {
            missing.add(name);
        }
    }
}
