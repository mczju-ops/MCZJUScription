package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.arena.ArenaBootstrap;
import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczju.mczjuscription.game.session.MatchSetup;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.lobby.InscriptionLeaderboards;
import com.github.mczju.mczjuscription.lobby.InscriptionRunScoring;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.AbstractGame;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 邪恶冥刻游戏基类。
 * 2×2 玩法变体见 {@link PlayVariant}，由 {@link InscriptionGame} 通过参数区分（非多个 MGC gameId）。
 */
public abstract class AbstractInscriptionGame extends AbstractGame {

    /** 玩家数据统一命名空间（各变体共用统计） */
    public static final String DATA_ID = "inscription";

    protected InscriptionMatch match;

    public abstract MatchMode getMatchMode();

    public DeckMode getDeckMode() {
        return match != null ? match.setup().deckMode() : DeckMode.FREE_BUILD;
    }

    protected abstract MatchSetup buildMatchSetup(List<PlayerExt> players, InscriptionGameRoom room);

    @Override
    protected boolean onGameInit() {
        return true;
    }

    @Override
    protected void onGameStart() {
        List<PlayerExt> players = getPlayers();
        if (players.isEmpty()) {
            sender.error("无法开始：没有玩家。");
            MCZJUGameCore.getGameManager().abortGame(this);
            return;
        }

        InscriptionGameRoom room = resolveMatchRoom();
        if (room == null) {
            sender.error("无法开始：未找到对局场地配置。");
            MCZJUGameCore.getGameManager().abortGame(this);
            return;
        }
        MatchSetup setup = buildMatchSetup(players, room);
        match = new InscriptionMatch(this, setup);
        match.life().resetForNewMatch(
                setup.candlesFor(MatchSide.PLAYER),
                setup.candlesFor(MatchSide.ENEMY)
        );

        ArenaBootstrap.bindForMatch(match, room, match.humanParticipants(), sender);

        for (ParticipantState participant : match.humanParticipants()) {
            participant.player().ifPresent(ext -> {
                if (room.spawnAt != null) {
                    ext.player().teleport(room.spawnAt);
                }
                ext.resetState();
                participant.hand().clear();
                InscriptionItems.giveStarterKit(ext, setup.deckMode());
            });
        }

        match.start();
    }

    @Override
    protected void onGameCancel() {
        stripAllPlayerInventories();
        match = null;
    }

    @Override
    protected void onGameAbort() {
        if (match != null) match.cleanup();
        stripAllPlayerInventories();
        match = null;
    }

    @Override
    protected void onGameEnd() {
        if (match != null) {
            recordPlayerStats(match.matchWinner());
            match.cleanup();
        }
        stripAllPlayerInventories();
        match = null;
    }

    /** 对局结束/取消时收回 MGC 卡牌与工具，避免 profile 或背包残留到下一局。 */
    private void stripAllPlayerInventories() {
        Set<UUID> stripped = new HashSet<>();
        for (PlayerExt ext : getPlayers()) {
            stripPlayer(ext.player(), stripped);
        }
        if (match != null) {
            for (ParticipantState human : match.humanParticipants()) {
                human.player().ifPresent(ext -> stripPlayer(ext.player(), stripped));
            }
        }
    }

    private static void stripPlayer(Player player, Set<UUID> stripped) {
        if (player == null || !stripped.add(player.getUniqueId())) {
            return;
        }
        InscriptionItems.stripPlayerInventory(player);
        // 晚一 tick 再清一次，避免 MGC 切换 profile 时把局内物品写回背包
        Bukkit.getScheduler().runTaskLater(MCZJUScriptionPlugin.getInstance(), () -> {
            if (player.isOnline()) {
                InscriptionItems.stripPlayerInventory(player);
            }
        }, 2L);
    }

    protected void recordPlayerStats(MatchSide winner) {
        for (PlayerExt playerExt : getPlayers()) {
            InscriptionPlayerData data = playerExt.getData(DATA_ID, InscriptionPlayerData.class);
            MatchSide side = match.sideFor(playerExt.player());
            if (side != null && side == winner) {
                data.wins += 1;
                int runDifficulty = InscriptionRunScoring.computeRunDifficulty(match.setup(), data);
                InscriptionRunScoring.recordClear(data, runDifficulty);
            }
            data.gamesPlayed += 1;
            data.setModified(true);
        }
        InscriptionLeaderboards.refreshClearBoard();
    }

    /**
     * 默认结束并踢出 MGC 游戏；{@link InscriptionGame} 覆盖为返回 main 大厅。
     */
    public void onMatchFinished(MatchSide winner) {
        for (ParticipantState human : match.humanParticipants()) {
            human.player()
                    .ifPresent(
                            ext -> {
                                MatchSide side = match.sideFor(ext.player());
                                match.feedback().announceVictory(ext, side == winner);
                            });
        }
        recordPlayerStats(winner);
        MCZJUGameCore.getGameManager().endGame(this);
    }

    public InscriptionMatch match() {
        return match;
    }

  private InscriptionGameRoom resolveMatchRoom() {
        if (this instanceof InscriptionGame inscription) {
            InscriptionGameRoom dedicated = inscription.matchRoom();
            if (dedicated != null) {
                return dedicated;
            }
        }
        if (getGameRoom() instanceof InscriptionGameRoom room) {
            return room;
        }
        return null;
    }
}
