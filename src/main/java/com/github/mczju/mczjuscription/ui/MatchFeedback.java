package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczju.mczjuscription.game.turn.TurnPhase;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;

/** 对局内玩家反馈：阶段/回合等用聊天栏；瞬时提示用 Action Bar。 */
public final class MatchFeedback {

    private final InscriptionMatch match;

    public MatchFeedback(InscriptionMatch match) {
        this.match = match;
    }

    public void announcePhase(TurnPhase phase, int turnNumber) {
        String label = phase.displayName();
        if (match.deckMode() == DeckMode.SHOP && phase == TurnPhase.PLAY) {
            label = "整备阶段";
        }
        chatAll("<gray>第 %d 回合 · <aqua>%s".formatted(turnNumber, label));
    }

    public void announceRoundWon(String loserLabel) {
        chatAll("<gold>小局结束 · <gray>%s 失去一根蜡烛".formatted(loserLabel));
    }

    public void announceVictory(PlayerExt player, boolean won) {
        if (won) {
            player.sender().success("<green>胜利");
        } else {
            player.sender().error("<red>败北");
        }
    }

    public void actionBarWarn(String miniMessage) {
        forEachHuman(ext -> ext.actionBarSender().warn(miniMessage));
    }

    public void actionBarInfo(String miniMessage) {
        forEachHuman(ext -> ext.actionBarSender().info(miniMessage));
    }

    public void announceInfo(String miniMessage) {
        chatAll(miniMessage);
    }

    private void chatAll(String miniMessage) {
        forEachHuman(ext -> ext.sender().info(miniMessage));
    }

    private void forEachHuman(java.util.function.Consumer<PlayerExt> action) {
        for (ParticipantState human : match.humanParticipants()) {
            human.player().ifPresent(action);
        }
    }
}
