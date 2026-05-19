package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczjuops.mczjugamecore.game.AbstractGame;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import org.bukkit.entity.Player;

/** 解析玩家当前邪恶冥刻游戏实例（大厅或对局）。 */
public final class InscriptionGameAccess {

    private InscriptionGameAccess() {}

    public static AbstractInscriptionGame resolveGame(PlayerExt ext) {
        if (!ext.isInGame()) {
            return null;
        }
        AbstractGame game = ext.getGame();
        if (game instanceof AbstractInscriptionGame inscription) {
            return inscription;
        }
        return null;
    }

    public static InscriptionGame resolveInscriptionGame(PlayerExt ext) {
        AbstractInscriptionGame game = resolveGame(ext);
        if (game instanceof InscriptionGame inscription) {
            return inscription;
        }
        return null;
    }

    public static AbstractInscriptionGame resolveGame(Player player) {
        return resolveGame(new PlayerExt(player));
    }

    public static boolean isInHub(PlayerExt ext) {
        InscriptionGame game = resolveInscriptionGame(ext);
        return game != null && game.isHubPhase();
    }

    public static boolean isInHub(Player player) {
        return isInHub(new PlayerExt(player));
    }

    public static InscriptionMatch resolveMatch(Player player) {
        AbstractInscriptionGame game = resolveGame(player);
        if (game == null) {
            return null;
        }

        GameState state = game.getState();
        if (state != GameState.RUNNING && state != GameState.STATING) {
            return null;
        }

        InscriptionMatch match = game.match();
        if (match == null || match.isMatchOver()) {
            return null;
        }
        return match;
    }
}
