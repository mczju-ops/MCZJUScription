package com.github.mczju.mczjuscription.game;

import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.manager.AbstractGameManager;
import com.github.mczjuops.mczjugamecore.game.room.AbstractGameRoom;
import com.github.mczjuops.mczjugamecore.game.room.GameRoomManager;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import java.lang.reflect.Method;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * 与 MCZJUGameCore 房间池 API 的兼容层：运行时探测，避免旧版 GameCore jar 导致 {@link NoSuchMethodError}。
 * <p>
 * 无法在 MCZJUScription 中继承 {@link com.github.mczjuops.mczjugamecore.game.manager.DefaultGameManager}
 *（GameCore 内部固定 {@code new DefaultGameManager()}）。
 * <p>
 * 大厅内开局请优先用 {@link com.github.mczju.mczjuscription.lobby.InscriptionHubMatchStarter}（插件内占用 play 房间）。
 * 本类 {@link #joinMatch} 仅在需要 quit 后新建实例时作可选回退（若服上 GameCore 带房间池 API）。
 */
public final class InscriptionGameCoreBridge {

    private static boolean roomPoolsAvailable;
    private static boolean joinGameFreshAvailable;
    private static boolean pinnedRoomJoinAvailable;

    private InscriptionGameCoreBridge() {}

    public static void init(JavaPlugin plugin) {
        roomPoolsAvailable = detectRoomPools();
        joinGameFreshAvailable = detectJoinGameFresh();
        pinnedRoomJoinAvailable = detectPinnedRoomJoin();
        if (roomPoolsAvailable && joinGameFreshAvailable) {
            plugin.getLogger().info("已启用 MGC 房间池（main / play10–19 / play20–24）。");
            if (pinnedRoomJoinAvailable) {
                plugin.getLogger().info("已启用 MGC 指定房间 join（roomId 参数）。");
            }
        } else {
            plugin.getLogger()
                    .warning(
                            "当前 MCZJUGameCore 不含完整房间池 API，请用本地最新 GameCore 执行 mvn install 并替换服内 jar；"
                                    + "否则开局可能仍占用 main。");
        }
    }

    public static boolean roomPoolsAvailable() {
        return roomPoolsAvailable;
    }

    public static void applyDefaultJoinRoomPool(GameMeta.Builder builder, Set<String> hubOnly) {
        if (!roomPoolsAvailable) {
            return;
        }
        try {
            Method method = builder.getClass().getMethod("defaultJoinRoomPool", Set.class);
            method.invoke(builder, hubOnly);
        } catch (ReflectiveOperationException ignored) {
            roomPoolsAvailable = false;
        }
    }

    /** 进入大厅；{@code roomId} 为 {@code null} 时仅 {@link InscriptionRoomPools#HUB}。 */
    public static void joinHub(PlayerExt player) {
        joinHub(player, null);
    }

    public static void joinHub(PlayerExt player, @Nullable String roomId) {
        joinGame(player, InscriptionGame.GAME_ID, InscriptionRoomPools.HUB_ONLY, roomId);
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
        if (!roomPoolsAvailable) {
            InscriptionPendingMatch.clear(player);
            player.sender()
                    .error(
                            "<red>服务器 MCZJUGameCore 版本过旧，无法按 play10–24 分配场地。"
                                    + " 请更新 GameCore 与 MCZJUScription 后完全重启。");
            return;
        }
        if (InscriptionPendingMatch.peek(player) == null) {
            InscriptionPendingMatch.set(player, variant);
        }
        Set<String> pool = InscriptionRoomPools.forVariant(variant);
        if (joinGameFreshAvailable) {
            joinGameFresh(player, InscriptionGame.GAME_ID, pool, roomId);
            return;
        }
        joinGame(player, InscriptionGame.GAME_ID, poolFor(roomId, pool), roomId);
    }

    public static void joinGame(
            PlayerExt player, String gameId, @Nullable Set<String> roomPool, @Nullable String roomId) {
        if (roomPoolsAvailable && (roomPool != null || roomId != null)) {
            if (pinnedRoomJoinAvailable && roomId != null) {
                try {
                    Method method =
                            AbstractGameManager.class.getMethod(
                                    "joinGame",
                                    PlayerExt.class,
                                    String.class,
                                    Set.class,
                                    String.class);
                    method.invoke(
                            MCZJUGameCore.getGameManager(), player, gameId, roomPool, roomId);
                    return;
                } catch (ReflectiveOperationException ignored) {
                    pinnedRoomJoinAvailable = false;
                }
            }
            if (roomPool != null) {
                try {
                    Method method =
                            AbstractGameManager.class.getMethod(
                                    "joinGame", PlayerExt.class, String.class, Set.class);
                    method.invoke(MCZJUGameCore.getGameManager(), player, gameId, poolFor(roomId, roomPool));
                    return;
                } catch (ReflectiveOperationException ignored) {
                    roomPoolsAvailable = false;
                }
            }
        }
        if (roomPool != null && !roomPool.equals(InscriptionRoomPools.HUB_ONLY)) {
            player.sender().error("<red>无法按指定场地池加入游戏，请更新 MCZJUGameCore。");
            return;
        }
        MCZJUGameCore.getGameManager().joinGame(player, gameId);
    }

    private static void joinGameFresh(
            PlayerExt player, String gameId, @Nullable Set<String> roomPool, @Nullable String roomId) {
        if (pinnedRoomJoinAvailable && roomId != null) {
            try {
                Method method =
                        AbstractGameManager.class.getMethod(
                                "joinGameFresh",
                                PlayerExt.class,
                                String.class,
                                Set.class,
                                String.class);
                method.invoke(MCZJUGameCore.getGameManager(), player, gameId, roomPool, roomId);
                return;
            } catch (ReflectiveOperationException ignored) {
                pinnedRoomJoinAvailable = false;
            }
        }
        try {
            Method method =
                    AbstractGameManager.class.getMethod(
                            "joinGameFresh", PlayerExt.class, String.class, Set.class);
            method.invoke(MCZJUGameCore.getGameManager(), player, gameId, poolFor(roomId, roomPool));
        } catch (ReflectiveOperationException ex) {
            joinGameFreshAvailable = false;
            joinGame(player, gameId, roomPool, roomId);
        }
    }

    private static Set<String> poolFor(@Nullable String roomId, @Nullable Set<String> fallback) {
        if (roomId != null && !roomId.isBlank()) {
            return Set.of(roomId.trim());
        }
        return fallback;
    }

    public static @Nullable AbstractGameRoom getLeisureGameRoom(String gameId, @Nullable Set<String> roomPool) {
        return getLeisureGameRoom(gameId, roomPool, null);
    }

    public static @Nullable AbstractGameRoom getLeisureGameRoom(
            String gameId, @Nullable Set<String> roomPool, @Nullable String roomId) {
        if (roomId != null && !roomId.isBlank()) {
            AbstractGameRoom pinned =
                    MCZJUGameCore.getGameRoomManager().getLeisureGameRoomByName(gameId, roomId.trim());
            if (pinned != null) {
                return pinned;
            }
        }
        if (roomPoolsAvailable && roomPool != null) {
            try {
                Method method =
                        GameRoomManager.class.getMethod("getLeisureGameRoom", String.class, Set.class);
                return (AbstractGameRoom)
                        method.invoke(MCZJUGameCore.getGameRoomManager(), gameId, roomPool);
            } catch (ReflectiveOperationException ignored) {
                roomPoolsAvailable = false;
            }
        }
        return MCZJUGameCore.getGameRoomManager().getLeisureGameRoom(gameId);
    }

    private static boolean detectRoomPools() {
        try {
            GameMeta.Builder.class.getMethod("defaultJoinRoomPool", Set.class);
            GameRoomManager.class.getMethod("getLeisureGameRoom", String.class, Set.class);
            AbstractGameManager.class.getMethod("joinGame", PlayerExt.class, String.class, Set.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean detectJoinGameFresh() {
        try {
            AbstractGameManager.class.getMethod(
                    "joinGameFresh", PlayerExt.class, String.class, Set.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static boolean detectPinnedRoomJoin() {
        try {
            AbstractGameManager.class.getMethod(
                    "joinGameFresh", PlayerExt.class, String.class, Set.class, String.class);
            GameRoomManager.class.getMethod("getLeisureGameRoomByName", String.class, String.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
