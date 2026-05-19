package com.github.mczju.mczjuscription.player;

import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.strategy.PlayerQuitReason;
import org.bukkit.entity.Player;

/** 单人模式主动退出：聊天确认后判负并结束对局。 */
public final class InscriptionLeaveSupport {

    private InscriptionLeaveSupport() {}

    public static boolean isSoloMatchInProgress(Player player) {
        AbstractInscriptionGame game = InscriptionGameAccess.resolveGame(player);
        if (game == null || game.getMatchMode() != MatchMode.SOLO_PVE) {
            return false;
        }
        if (game.getState() != GameState.RUNNING) {
            return false;
        }
        InscriptionMatch match = game.match();
        return match != null && !match.isMatchOver();
    }

    public static boolean isLeaveCommand(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String[] parts = message.trim().split("\\s+");
        if (parts.length < 2) {
            return false;
        }
        String cmd = parts[0].startsWith("/") ? parts[0].substring(1) : parts[0];
        return cmd.equalsIgnoreCase("mgc") && parts[1].equalsIgnoreCase("leave");
    }

    /** 拦截 leave 指令，在聊天栏提示并打开确认菜单。 */
    public static void promptLeaveConfirm(Player player) {
        PlayerExt ext = new PlayerExt(player);
        ext.sender().warn("<yellow>退出对局将判负并结束游戏。");
        ext.sender().info("<gray>请在菜单中确认，或点击取消继续游戏。");
        new AlertMenu(player, () -> ext.quitGame(PlayerQuitReason.COMMAND_QUIT)).open();
    }
}
