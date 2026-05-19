package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.InscriptionGameCoreBridge;
import com.github.mczju.mczjuscription.game.InscriptionPendingMatch;
import com.github.mczju.mczjuscription.game.InscriptionPlayRoomAllocator;
import com.github.mczju.mczjuscription.game.InscriptionRoomPools;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.party.Party;
import com.github.mczjuops.mczjugamecore.player.strategy.PlayerQuitReason;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * 从大厅开局：优先在当前 {@link InscriptionGame} 实例上分配 play 房间（不改 GameCore）；
 * 仅当无法与大厅其他玩家隔离时，才回退到 quit + {@link InscriptionGameCoreBridge#joinMatch}。
 */
public final class InscriptionHubMatchStarter {

    private InscriptionHubMatchStarter() {}

    public static void startSolo(PlayerExt player, PlayVariant variant) {
        startSolo(player, variant, null);
    }

    public static void startSolo(PlayerExt player, PlayVariant variant, @Nullable String roomId) {
        if (!variant.isSolo()) {
            player.sender().error("无效的单人变体。");
            return;
        }
        if (player.isInParty()) {
            player.sender().warn("请先退出队伍再开始单人游戏。");
            return;
        }
        prepareSeats(player);
        start(player, variant, roomId);
    }

    public static void startDuel(Party party, PlayVariant variant) {
        startDuel(party, variant, null);
    }

    public static void startDuel(Party party, PlayVariant variant, @Nullable String roomId) {
        if (!variant.isDuel() || party.getAllPlayer().size() != 2) {
            party.getLeader().sender().warn("双人模式需要 2 人队伍。");
            return;
        }
        PlayerExt leader = party.getLeader();
        if (!leader.isPartyLeader()) {
            return;
        }
        for (PlayerExt member : party.getAllPlayer()) {
            prepareSeats(member);
        }
        start(leader, variant, roomId);
    }

    private static void prepareSeats(PlayerExt player) {
        HubSeatService.dismount(player.player());
        HubSeatService.release(player.getUniqueId());
    }

    private static void start(PlayerExt actor, PlayVariant variant, @Nullable String roomId) {
        if (!InscriptionPlayRoomAllocator.hasLeisureRoom(variant, roomId)) {
            actor.sender()
                    .warn("<red>" + InscriptionRoomPools.poolLabel(variant) + " 已满，请稍后再试。");
            return;
        }

        InscriptionGame game = InscriptionGameAccess.resolveInscriptionGame(actor);
        if (game == null || !game.isHubPhase()) {
            actor.sender().warn("请先进入" + com.github.mczju.mczjuscription.InscriptionBranding.TITLE + " 大厅。");
            return;
        }

        if (canStartInPlace(game, variant, actor)) {
            if (game.beginMatch(variant, roomId)) {
                game.getGameWaitStrategy().startGame();
            }
            return;
        }

        InscriptionPendingMatch.set(actor, variant);
        actor.quitGame(PlayerQuitReason.COMMAND_QUIT);
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> InscriptionGameCoreBridge.joinMatch(actor, variant, roomId),
                        2L);
    }

    /** 当前实例内只有即将开局的玩家（单人或整队），可在原实例上切换为对局。 */
    private static boolean canStartInPlace(
            InscriptionGame game, PlayVariant variant, PlayerExt actor) {
        if (game.getState() != GameState.RUNNING) {
            return true;
        }
        List<PlayerExt> present = game.getPlayers();
        if (variant.isSolo()) {
            return present.size() == 1 && present.getFirst().equals(actor);
        }
        Party party = actor.getParty();
        if (party == null || party.getAllPlayer().size() != 2) {
            return false;
        }
        Set<PlayerExt> expected = new HashSet<>(party.getAllPlayer());
        return present.size() == expected.size() && expected.containsAll(present);
    }
}
