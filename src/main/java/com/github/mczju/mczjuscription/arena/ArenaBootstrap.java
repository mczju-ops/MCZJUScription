package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczjuops.mczjugamecore.utils.sender.Sender;
import org.bukkit.entity.Player;

import java.util.List;

/** 对局开始时绑定战斗场地（房间配置或玩家脚下临时场地）。 */
public final class ArenaBootstrap {

    private ArenaBootstrap() {}

    public static void bindForMatch(
            InscriptionMatch match,
            InscriptionGameRoom room,
            List<ParticipantState> humans,
            Sender sender
    ) {
        if (match.arena() != null) return;

        if (room.hasConfiguredArena(match.mode())) {
            try {
                BattleArena arena = BattleArena.fromRoom(room, match.mode(), match.deckMode());
                ArenaManager.bindSharedRoomArena(arena);
                match.bindArena(arena);
                warnMissingStaging(match, arena, sender);
            } catch (IllegalStateException ex) {
                sender.warn("场地配置无效：" + ex.getMessage());
                bindAtPlayerFallback(match, humans, sender);
            }
            return;
        }

        bindAtPlayerFallback(match, humans, sender);
    }

    private static void bindAtPlayerFallback(
            InscriptionMatch match, List<ParticipantState> humans, Sender sender) {
        humans.stream()
                .findFirst()
                .flatMap(ParticipantState::player)
                .map(ext -> ext.player())
                .ifPresentOrElse(
                        player -> bindAtPlayer(match, player, sender),
                        () -> sender.warn("无法生成场地：没有玩家。")
                );
    }

    public static void bindAtPlayer(InscriptionMatch match, Player player, Sender sender) {
        BattleArena arena = ArenaManager.createAtPlayer(player);
        match.bindArena(arena);
        match.feedback().actionBarInfo("<gray>已生成临时场地");
    }

    private static void warnMissingStaging(InscriptionMatch match, BattleArena arena, Sender sender) {
        var layout = arena.layout();
        if (layout == null) {
            return;
        }
        var playerStaging = layout.staging(com.github.mczju.mczjuscription.game.match.MatchSide.PLAYER);
        if (playerStaging == null
                || (!layout.hasBellStrip(com.github.mczju.mczjuscription.game.match.MatchSide.PLAYER)
                        && playerStaging.clock == null)) {
            match.feedback().actionBarWarn("<yellow>未配置敲钟位置");
        }
        if (match.deckMode() == DeckMode.SHOP
                && playerStaging != null
                && playerStaging.shopSlot == null
                && playerStaging.shopVillager == null) {
            match.feedback().actionBarWarn("<yellow>未配置商店槽位或商店村民");
        }
    }
}
