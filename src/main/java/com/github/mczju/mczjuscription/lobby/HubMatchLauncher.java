package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczju.mczjuscription.game.variant.DuelFreeBuildGame;
import com.github.mczju.mczjuscription.game.variant.DuelShopGame;
import com.github.mczju.mczjuscription.game.variant.SoloFreeBuildGame;
import com.github.mczju.mczjuscription.game.variant.SoloShopGame;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.party.Party;
import com.github.mczjuops.mczjugamecore.player.strategy.PlayerQuitReason;
import org.bukkit.Bukkit;

/** 离开大厅并加入对局变体。 */
public final class HubMatchLauncher {

    private HubMatchLauncher() {}

    public static void startSolo(PlayerExt player, PlayVariant variant) {
        if (!variant.isSolo()) {
            player.sender().error("无效的单人变体。");
            return;
        }
        if (player.isInParty()) {
            player.sender().warn("请先退出队伍再开始单人游戏。");
            return;
        }
        String gameId = gameIdFor(variant);
        if (gameId == null) {
            player.sender().error("该模式尚未注册。");
            return;
        }
        leaveHubThenJoin(player, gameId);
    }

    public static void startDuel(Party party, PlayVariant variant) {
        if (!variant.isDuel()) {
            return;
        }
        if (party.getAllPlayer().size() != 2) {
            party.getLeader().sender().warn("双人模式需要 2 人队伍。");
            return;
        }
        PlayerExt leader = party.getLeader();
        if (!leader.isPartyLeader()) {
            return;
        }
        String gameId = gameIdFor(variant);
        if (gameId == null) {
            leader.sender().error("双人模式尚未注册，请联系管理员。");
            return;
        }
        for (PlayerExt member : party.getAllPlayer()) {
            HubSeatService.dismount(member.player());
            HubSeatService.release(member.getUniqueId());
        }
        leader.quitGame(PlayerQuitReason.COMMAND_QUIT);
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> MCZJUGameCore.getGameManager().joinGame(leader, gameId),
                        2L);
    }

    private static void leaveHubThenJoin(PlayerExt player, String gameId) {
        HubSeatService.dismount(player.player());
        HubSeatService.release(player.getUniqueId());
        player.quitGame(PlayerQuitReason.COMMAND_QUIT);
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> MCZJUGameCore.getGameManager().joinGame(player, gameId),
                        2L);
    }

    private static String gameIdFor(PlayVariant variant) {
        if (variant == PlayVariant.SOLO_FREE_BUILD) {
            return SoloFreeBuildGame.GAME_ID;
        }
        if (variant == PlayVariant.SOLO_SHOP) {
            return SoloShopGame.GAME_ID;
        }
        if (variant == PlayVariant.DUEL_FREE_BUILD) {
            return DuelFreeBuildGame.GAME_ID;
        }
        if (variant == PlayVariant.DUEL_SHOP) {
            return DuelShopGame.GAME_ID;
        }
        return null;
    }

    public static boolean isDuelRegistered() {
        return MCZJUGameCore.getGameManager().getRegisteredGameIds().contains(DuelShopGame.GAME_ID);
    }
}
