package com.github.mczju.mczjuscription.game.strategy;

import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionPendingMatch;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.game.GameState;
import com.github.mczjuops.mczjugamecore.game.strategy.wait.DefaultGameWaitStrategy;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.party.Party;

/**
 * 邪恶冥刻统一等待逻辑。
 * <p>
 * 继承 {@link DefaultGameWaitStrategy}（其构造器为 public），避免跨插件 ClassLoader 无法调用
 * {@link com.github.mczjuops.mczjugamecore.game.strategy.wait.GameWaitStrategy} 包内构造器的问题。
 */
public final class InscriptionGameWaitStrategy extends DefaultGameWaitStrategy {

    private static final int HUB_PLAYER_LIMIT = 67656;

    public InscriptionGameWaitStrategy(InscriptionGame game) {
        super(game, HUB_PLAYER_LIMIT);
    }

    private InscriptionGame inscription() {
        return (InscriptionGame) game;
    }

    @Override
    public boolean onPlayerJoin(PlayerExt player) {
        PlayVariant pending = InscriptionPendingMatch.consume(player);
        if (pending != null) {
            return startMatchJoin(player, pending);
        }
        return joinHub(player);
    }

    @Override
    public boolean onPartyJoin(Party party) {
        PlayerExt leader = party.getLeader();
        PlayVariant pending = InscriptionPendingMatch.consume(leader);
        if (pending != null) {
            if (!pending.isDuel()) {
                leader.sender().warn("该模式只能单人开始。");
                return false;
            }
            if (party.getAllPlayer().size() != 2) {
                leader.sender().warn("双人模式需要 2 人队伍。");
                return false;
            }
            InscriptionGame ig = inscription();
            if (!ig.beginMatch(pending, null)) {
                return false;
            }
            startGame();
            return true;
        }
        leader.sender().warn("请在大厅双人座组队并选模式，或使用 /isc start duel …");
        return false;
    }

    @Override
    public void onPlayerLeave(PlayerExt player) {
        InscriptionGame ig = inscription();
        if (ig.isHubPhase()) {
            ig.onHubPlayerLeave(player);
        }
    }

    @Override
    public void tryStart() {
        InscriptionGame ig = inscription();
        if (ig.getActiveVariant() != null && game.getState() == GameState.WAITING) {
            startGame();
        }
    }

    private boolean startMatchJoin(PlayerExt player, PlayVariant pending) {
        if (pending.isSolo()) {
            if (player.isInParty()) {
                player.sender().warn("请先退出队伍再开始单人游戏。");
                return false;
            }
        } else {
            player.sender().warn("双人模式请由队长带队伍加入。");
            return false;
        }
        InscriptionGame ig = inscription();
        if (!ig.beginMatch(pending, null)) {
            return false;
        }
        startGame();
        return true;
    }

    private boolean joinHub(PlayerExt player) {
        InscriptionGame ig = inscription();
        ig.enterHub(player);
        if (game.getState() == GameState.WAITING) {
            startGame();
        }
        return true;
    }
}
