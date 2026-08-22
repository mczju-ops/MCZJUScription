package com.github.mczju.mczjuscription.game;

import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.room.AbstractGameRoom;
import com.github.mczjuops.mczjugamecore.game.room.GameRoomState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * 与 MCZJUGameCore 1.0.8（paper-api 26.2 架构）的对接层。
 * <p>
 * 1.0.8 移除了旧版房间池 API（{@code joinGameFresh} / {@code defaultJoinRoomPool} / room pool 集合），
 * 改为按房间名固定加入：{@code AbstractGameManager#joinGame(PlayerExt, String, String)}。
 * 本类据此将「大厅 main」与「对局 play10–24」按房间名固定路由，避免撞上共享大厅实例。
 */
public final class InscriptionGameCoreBridge {

    private InscriptionGameCoreBridge() {}

    public static void init(JavaPlugin plugin) {
        plugin.getLogger()
                .info(
                        "已接入 MCZJUGameCore 1.0.8：joinGame 按房间名固定（%s / play10–24）。"
                                .formatted(InscriptionRoomPools.HUB));
    }

    /** 进入大厅；{@code roomId} 为 {@code null} 时默认 {@link InscriptionRoomPools#HUB}（main）。 */
    public static void joinHub(PlayerExt player) {
        joinHub(player, InscriptionRoomPools.HUB);
    }

    public static void joinHub(PlayerExt player, @Nullable String roomId) {
        String room = roomId != null && !roomId.isBlank() ? roomId.trim() : InscriptionRoomPools.HUB;
        MCZJUGameCore.getGameManager().joinGame(player, InscriptionGame.GAME_ID, room);
    }

    /**
     * 对局结束后回到 main：固定加入 main 房间。玩家已不在对局实例中，
     * 因此 joinGame 会命中 main 上的等待大厅实例（或新建），无需旧版 joinGameFresh。
     */
    public static void joinHubFresh(PlayerExt player) {
        joinHubFresh(player, InscriptionRoomPools.HUB);
    }

    public static void joinHubFresh(PlayerExt player, @Nullable String roomId) {
        joinHub(player, roomId);
    }

    private static final int JOIN_MATCH_MAX_ATTEMPTS = 40;

    /** 开局；{@code roomId} 为 {@code null} 时从 solo/duel 池自动分配。 */
    public static void joinMatch(PlayerExt player, PlayVariant variant) {
        joinMatch(player, variant, null);
    }

    public static void joinMatch(PlayerExt player, PlayVariant variant, @Nullable String roomId) {
        joinMatchWhenReady(player, variant, roomId, 0);
    }

    private static void joinMatchWhenReady(
            PlayerExt player, PlayVariant variant, @Nullable String roomId, int attempt) {
        if (player.isInGame()) {
            if (attempt >= JOIN_MATCH_MAX_ATTEMPTS) {
                InscriptionPendingMatch.clear(player);
                player.sender().error("<red>离开大厅超时，请稍后再试。");
                return;
            }
            Bukkit.getScheduler()
                    .runTaskLater(
                            MCZJUScriptionPlugin.getInstance(),
                            () -> joinMatchWhenReady(player, variant, roomId, attempt + 1),
                            2L);
            return;
        }
        if (InscriptionPendingMatch.peek(player) == null) {
            InscriptionPendingMatch.set(player, variant);
        }
        String target =
                roomId != null && !roomId.isBlank() ? roomId.trim() : firstLeisureRoom(variant);
        if (target == null) {
            InscriptionPendingMatch.clear(player);
            player.sender()
                    .error("<red>" + InscriptionRoomPools.poolLabel(variant) + " 已满，请稍后再试。");
            return;
        }
        MCZJUGameCore.getGameManager().joinGame(player, InscriptionGame.GAME_ID, target);
    }

    /** 从变体房间池中挑第一个 READY 的 play 房间名；全部占用返回 {@code null}。 */
    private static @Nullable String firstLeisureRoom(PlayVariant variant) {
        for (String name : InscriptionRoomPools.forVariant(variant)) {
            AbstractGameRoom room =
                    MCZJUGameCore.getGameRoomManager().getGameRoom(InscriptionGame.GAME_ID, name);
            if (room != null && room.getState() == GameRoomState.READY) {
                return name;
            }
        }
        return null;
    }
}
