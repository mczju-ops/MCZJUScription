package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.InscriptionGameCoreBridge;
import com.github.mczju.mczjuscription.game.InscriptionRoomPools;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.strategy.PlayerQuitReason;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;

/**
 * 对局结束后将玩家送回 {@link InscriptionRoomPools#HUB} 大厅。
 * <p>
 * play 场地对局须先 {@code quitGame} 再 {@link InscriptionGameCoreBridge#joinHubFresh} 加入 main 上的
 * MGC 游戏实例，否则无法交互大厅 UI / 座椅；仅在 main 内 {@code /mgc leave} 才算真正退出小游戏。
 */
public final class HubReturnService {

    private static final int REJOIN_HUB_MAX_ATTEMPTS = 40;

    private HubReturnService() {}

    public static void returnAfterMatch(InscriptionGame game, MatchSide winner, List<PlayerExt> humans) {
        if (humans.isEmpty()) {
            game.clearMatchPhase();
            return;
        }

        boolean inPlaceOnMain = isBoundToMainHub(game);
        for (PlayerExt player : humans) {
            InscriptionItems.stripPlayerInventory(player.player());
            MatchSide side = game.match() != null ? game.match().sideFor(player.player()) : null;
            if (side != null && side != winner) {
                player.sender().warn("<gray>已回到大厅，可重新选座再战。");
            } else if (side != null) {
                player.sender().info("<gray>已回到大厅。");
            } else {
                player.sender().info("<gray>已回到大厅。");
            }
        }

        game.clearMatchPhase();

        if (inPlaceOnMain) {
            game.setState(GameState.RUNNING);
            for (PlayerExt player : humans) {
                game.enterHub(player);
            }
            game.refreshHubSession();
            return;
        }

        List<PlayerExt> copy = new ArrayList<>(humans);
        final InscriptionGame playInstance = game;
        for (PlayerExt player : copy) {
            leaveCurrentGame(player);
        }
        scheduleAbortPlayInstance(playInstance);
        for (PlayerExt player : copy) {
            rejoinMainHub(player, 0);
        }
    }

    /** 从 play 对局实例退出后，加入 main 大厅的 MGC 游戏（带重试）。 */
    public static void rejoinMainHub(PlayerExt player) {
        rejoinMainHub(player, 0);
    }

    private static void rejoinMainHub(PlayerExt player, int attempt) {
        if (!player.player().isOnline()) {
            return;
        }
        if (InscriptionGameAccess.isInHub(player)) {
            return;
        }
        if (player.isInGame()) {
            if (attempt >= REJOIN_HUB_MAX_ATTEMPTS) {
                player.sender()
                        .error("<red>返回大厅失败，请使用 <white>/isc hub</white> 或 <white>/mgc</white> 重新进入。");
                return;
            }
            leaveCurrentGame(player);
            Bukkit.getScheduler()
                    .runTaskLater(
                            MCZJUScriptionPlugin.getInstance(),
                            () -> rejoinMainHub(player, attempt + 1),
                            2L);
            return;
        }
        if (attempt >= REJOIN_HUB_MAX_ATTEMPTS) {
            player.sender()
                    .error("<red>返回大厅失败，请使用 <white>/isc hub</white> 或 <white>/mgc</white> 重新进入。");
            return;
        }
        InscriptionGameCoreBridge.joinHubFresh(player, InscriptionRoomPools.HUB);
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> {
                            if (!player.player().isOnline()) {
                                return;
                            }
                            if (InscriptionGameAccess.isInHub(player)) {
                                return;
                            }
                            rejoinMainHub(player, attempt + 1);
                        },
                        5L);
    }

    private static void leaveCurrentGame(PlayerExt player) {
        if (player.isInGame()) {
            player.quitGame(PlayerQuitReason.COMMAND_QUIT);
        }
    }

    private static void scheduleAbortPlayInstance(InscriptionGame playInstance) {
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> {
                            if (playInstance.getPlayers().isEmpty()) {
                                MCZJUGameCore.getGameManager().abortGame(playInstance);
                            }
                        },
                        20L);
    }

    private static boolean isBoundToMainHub(InscriptionGame game) {
        return game.getGameRoom() != null
                && InscriptionRoomPools.HUB.equals(game.getGameRoom().getRoomName());
    }
}
