package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczju.mczjuscription.game.session.MatchSetup;
import com.github.mczju.mczjuscription.game.session.MatchSetupFactory;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczju.mczjuscription.InscriptionBranding;
import com.github.mczju.mczjuscription.game.strategy.InscriptionGameWaitStrategy;
import com.github.mczju.mczjuscription.game.InscriptionPendingMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczju.mczjuscription.lobby.HubDisplayBootstrap;
import com.github.mczju.mczjuscription.lobby.HubReturnService;
import com.github.mczju.mczjuscription.lobby.HubSeatService;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.room.GameRoomManager;
import com.github.mczju.mczjuscription.player.InscriptionPlayerQuitStrategy;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.MidGameJoinable;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.GameWaitStrategy;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.strategy.AbstractPlayerQuitStrategy;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MGC 中唯一注册的「{@link InscriptionBranding#TITLE}」游戏。
 * <p>
 * 共享大厅与四种玩法变体共用 {@link #GAME_ID}；变体由 {@link InscriptionPendingMatch} + 指令/座位触发，
 * 不再注册四个独立 gameId。
 */
public final class InscriptionGame extends AbstractInscriptionGame implements MidGameJoinable {

    public static final String GAME_ID = "inscription";

    private final GameWaitStrategy waitStrategy = new InscriptionGameWaitStrategy(this);

    @Nullable
    private PlayVariant activeVariant;

    /** 对局实际场地（MGC 实例可能仍绑定 {@link InscriptionRoomPools#HUB}）。 */
    @Nullable
    private InscriptionGameRoom matchRoom;

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    public GameMeta getGameMeta() {
        GameMeta.Builder builder =
                GameMeta.builder()
                .displayName(InscriptionBranding.TITLE_COLORED)
                .icon(Material.CANDLE)
                .author("<green>MCZJU")
                .description(
                        List.of(
                                "<gray>等待大厅 · 构牌 / 选座开局",
                                "<gray>单人：商店或自由构牌",
                                "<gray>双人：组队后选模式",
                                "<yellow>▶ 点击进入大厅"));
        InscriptionGameCoreBridge.applyDefaultJoinRoomPool(builder, InscriptionRoomPools.HUB_ONLY);
        return builder.build();
    }

    @Override
    public GameWaitStrategy getGameWaitStrategy() {
        return waitStrategy;
    }

    @Override
    public @NotNull AbstractPlayerQuitStrategy getPlayerQuitStrategy() {
        return new InscriptionPlayerQuitStrategy(this);
    }

    @Override
    public MatchMode getMatchMode() {
        if (activeVariant == null) {
            return MatchMode.SOLO_PVE;
        }
        return activeVariant.matchMode();
    }

    @Override
    public DeckMode getDeckMode() {
        if (match != null) {
            return super.getDeckMode();
        }
        return activeVariant != null ? activeVariant.deckMode() : DeckMode.FREE_BUILD;
    }

    @Nullable
    public PlayVariant getActiveVariant() {
        return activeVariant;
    }

    public void setActiveVariant(@Nullable PlayVariant activeVariant) {
        this.activeVariant = activeVariant;
    }

    @Nullable
    public InscriptionGameRoom matchRoom() {
        return matchRoom;
    }

    /**
     * 在当前游戏实例上占用 play 场地并进入对局阶段（无需 quit / joinGameFresh）。
     *
     * @return 是否成功占用场地
     */
    public boolean beginMatch(PlayVariant variant, @Nullable String roomId) {
        if (activeVariant != null || match != null) {
            return false;
        }
        InscriptionGameRoom room = InscriptionPlayRoomAllocator.claim(variant, roomId, false);
        if (room == null) {
            sender.warn("<red>对局场地不可用，请稍后再试。");
            return false;
        }
        matchRoom = room;
        activeVariant = variant;
        return true;
    }

    public void clearMatchPhase() {
        InscriptionPlayRoomAllocator.release(matchRoom);
        matchRoom = null;
        activeVariant = null;
    }

    public boolean isHubPhase() {
        return match == null && activeVariant == null;
    }

    @Override
    protected boolean onGameInit() {
        return true;
    }

    @Override
    protected void onGameStart() {
        if (activeVariant == null) {
            startHubSession();
            return;
        }
        if (matchRoom == null && getGameRoom() != null && InscriptionRoomPools.HUB.equals(getGameRoom().getRoomName())) {
            sender.error("<red>对局场地未分配。请创建 play10–24 房间，或从大厅座位重新开局。");
            MCZJUGameCore.getGameManager().abortGame(this);
            return;
        }
        super.onGameStart();
        InscriptionGameRoom venue = matchRoom != null ? matchRoom : (InscriptionGameRoom) getGameRoom();
        if (venue != null) {
            sender.info("<gray>对局场地：<white>%s".formatted(venue.getRoomName()));
        }
    }

    @Override
    protected void onGameCancel() {
        clearMatchPhase();
        super.onGameCancel();
    }

    @Override
    protected void onGameAbort() {
        clearMatchPhase();
        super.onGameAbort();
    }

    @Override
    protected void onGameEnd() {
        super.onGameEnd();
        clearMatchPhase();
    }

    @Override
    public void onMatchFinished(MatchSide winner) {
        if (match == null) {
            return;
        }
        for (ParticipantState human : match.humanParticipants()) {
            human.player()
                    .ifPresent(
                            ext -> {
                                MatchSide side = match.sideFor(ext.player());
                                match.feedback().announceVictory(ext, side == winner);
                            });
        }
        recordPlayerStats(winner);

        List<PlayerExt> humans = new ArrayList<>();
        for (ParticipantState human : match.humanParticipants()) {
            human.player().ifPresent(humans::add);
        }

        HubReturnService.returnAfterMatch(this, winner, humans);

        match.cleanup();
        match = null;
    }

    public void refreshHubSession() {
        startHubSession();
    }

    @Override
    protected MatchSetup buildMatchSetup(List<PlayerExt> players, InscriptionGameRoom room) {
        if (activeVariant == null) {
            throw new IllegalStateException("对局变体未设置");
        }
        return MatchSetupFactory.create(activeVariant, players, room);
    }

    @Override
    public boolean onPlayerMidJoin(PlayerExt player) {
        if (InscriptionPendingMatch.peek(player) != null) {
            return false;
        }
        if (!isHubPhase()) {
            return false;
        }
        enterHub(player);
        return true;
    }

    public void enterHub(PlayerExt player) {
        player.switchProfile(GAME_ID);
        player.resetState();
        InscriptionGameRoom room = hubRoom();
        if (room != null) {
            // 展示实体按房间只生成一套；此处仅在缺失时补生成，避免每人进房刷一遍
            HubDisplayBootstrap.ensureHubDecor(room);
            if (room.spawnAt != null) {
                player.player().teleport(room.spawnAt);
            }
        } else {
            player.sender().warn("大厅房间未配置，请管理员创建 inscription 房间（如 main）。");
        }
        HubSeatService.release(player.getUniqueId());
        player.sender()
                .info(
                        "<gold>" + InscriptionBranding.TITLE + " 大厅</gold>：讲台构牌、赐福/诅咒区、排行榜、单人/双人座位。"
                                + " 管理员可用 <white>/isc start <solo|duel> <shop|free></white> 直接开局。");
    }

    public void onHubPlayerLeave(PlayerExt player) {
        HubSeatService.dismount(player.player());
        HubSeatService.release(player.getUniqueId());
        player.switchProfile(null);
    }

    private void startHubSession() {
        InscriptionGameRoom room = hubRoom();
        if (room == null) {
            return;
        }
        try {
            HubDisplayBootstrap.sync(room);
        } catch (Exception ex) {
            logger.error("大厅展示同步失败: %s".formatted(ex.getMessage()));
        }
    }

    @Nullable
    public InscriptionGameRoom hubRoom() {
        GameRoomManager rooms = MCZJUGameCore.getGameRoomManager();
        if (rooms.getGameRoom(GAME_ID, InscriptionRoomPools.HUB) instanceof InscriptionGameRoom main) {
            return main;
        }
        if (getGameRoom() instanceof InscriptionGameRoom bound
                && InscriptionRoomPools.HUB.equals(bound.getRoomName())) {
            return bound;
        }
        return null;
    }
}
