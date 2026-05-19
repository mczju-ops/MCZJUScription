package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
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

        if (room.hasConfiguredArena()) {
            BattleArena arena = BattleArena.fromRoom(room);
            ArenaManager.bindSharedRoomArena(arena);
            match.bindArena(arena);
            if (room.clockAt == null) {
                match.feedback().actionBarWarn("<yellow>未配置敲钟位置");
            }
            return;
        }

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
}
