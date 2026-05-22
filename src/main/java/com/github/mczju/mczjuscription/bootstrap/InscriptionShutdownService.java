package com.github.mczju.mczjuscription.bootstrap;

import com.github.mczju.mczjuscription.arena.ArenaManager;
import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.vfx.InkAuraVfx;
import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.lobby.HubSeatMarkerService;
import com.github.mczju.mczjuscription.lobby.HubSession;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.AbstractGame;
import com.github.mczjuops.mczjugamecore.game.manager.AbstractGameManager;
import com.github.mczjuops.mczjugamecore.game.manager.DefaultGameManager;
import com.github.mczjuops.mczjugamecore.game.room.AbstractGameRoom;
import com.github.mczjuops.mczjugamecore.game.room.GameRoomManager;
import com.github.mczjuops.mczjugamecore.game.room.GameRoomState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 关服 / 重载插件时结束对局、释放房间并清除遗留展示实体。 */
public final class InscriptionShutdownService {

    private InscriptionShutdownService() {}

    public static void shutdown(Logger logger) {
        int games = abortInscriptionGames(logger);
        int rooms = releaseOccupiedRooms();
        int hubDecor = despawnAllHubDecor();
        HubSession.shutdownAll();
        ArenaManager.cleanupAll();
        InkAuraVfx.stopAll();
        int entities = CreatureEntityService.purgeAllPluginEntities();
        if (games > 0 || rooms > 0 || hubDecor > 0 || entities > 0) {
            logger.info(
                    "邪恶冥刻关服清理：结束游戏 %d，释放房间 %d，大厅装饰 %d，移除实体 %d"
                            .formatted(games, rooms, hubDecor, entities));
        }
    }

    private static int abortInscriptionGames(Logger logger) {
        AbstractGameManager manager = MCZJUGameCore.getGameManager();
        List<AbstractGame> games = snapshotInscriptionGames(manager);
        int count = 0;
        for (AbstractGame game : games) {
            if (!(game instanceof InscriptionGame inscription)) {
                continue;
            }
            try {
                InscriptionMatch match = inscription.match();
                if (match != null) {
                    match.cleanup();
                }
                inscription.clearMatchPhase();
                manager.abortGame(game);
                count++;
            } catch (Exception ex) {
                logger.warning("结束游戏实例失败: " + ex.getMessage());
            }
        }
        return count;
    }

    private static List<AbstractGame> snapshotInscriptionGames(AbstractGameManager manager) {
        Set<AbstractGame> unique = new HashSet<>();
        if (manager instanceof DefaultGameManager defaultManager) {
            try {
                Field field = DefaultGameManager.class.getDeclaredField("gameList");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<AbstractGame> list = (List<AbstractGame>) field.get(defaultManager);
                synchronized (list) {
                    for (AbstractGame game : list) {
                        if (game instanceof InscriptionGame) {
                            unique.add(game);
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
                // 回退：仅扫描在线玩家
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            AbstractGame game = new PlayerExt(player).getGame();
            if (game instanceof InscriptionGame) {
                unique.add(game);
            }
        }
        return new ArrayList<>(unique);
    }

    private static int releaseOccupiedRooms() {
        GameRoomManager rooms = MCZJUGameCore.getGameRoomManager();
        int released = 0;
        for (String name : rooms.getGameRoomNames(InscriptionGame.GAME_ID)) {
            AbstractGameRoom room = rooms.getGameRoom(InscriptionGame.GAME_ID, name);
            if (room != null && room.getState() == GameRoomState.IN_GAME) {
                room.setState(GameRoomState.READY);
                released++;
            }
        }
        return released;
    }

    private static int despawnAllHubDecor() {
        GameRoomManager rooms = MCZJUGameCore.getGameRoomManager();
        int count = 0;
        for (String name : rooms.getGameRoomNames(InscriptionGame.GAME_ID)) {
            AbstractGameRoom raw = rooms.getGameRoom(InscriptionGame.GAME_ID, name);
            if (!(raw instanceof InscriptionGameRoom room)) {
                continue;
            }
            HubSeatMarkerService.despawn(room);
            count++;
        }
        return count;
    }
}
