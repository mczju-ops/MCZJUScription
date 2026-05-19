package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameCoreBridge;
import com.github.mczju.mczjuscription.game.InscriptionRoomPools;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.strategy.PlayerQuitReason;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;

/** 对局结束后将玩家送回 {@link InscriptionRoomPools#HUB} 大厅。 */
public final class HubReturnService {

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
            if (side != null) {
                if (side == winner) {
                    player.sender().info("<green>对局胜利！<gray>已返回等待大厅。");
                } else {
                    player.sender().warn("<red>对局失败。<gray>已返回等待大厅，可重新选座开局。");
                }
            } else {
                player.sender().info("<gray>已返回等待大厅。");
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
        for (PlayerExt player : copy) {
            if (player.isInGame()) {
                player.quitGame(PlayerQuitReason.COMMAND_QUIT);
            }
        }
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> {
                            for (PlayerExt player : copy) {
                                if (!player.player().isOnline()) {
                                    continue;
                                }
                                InscriptionGameCoreBridge.joinHub(player);
                            }
                        },
                        3L);
    }

    private static boolean isBoundToMainHub(InscriptionGame game) {
        return game.getGameRoom() != null
                && InscriptionRoomPools.HUB.equals(game.getGameRoom().getRoomName());
    }

}
