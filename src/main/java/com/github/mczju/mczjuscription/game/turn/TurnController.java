package com.github.mczju.mczjuscription.game.turn;

import com.github.mczju.mczjuscription.game.board.PreviewAdvanceSequence;
import com.github.mczju.mczjuscription.game.combat.CombatResolver;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.combat.PreCombatResolver;
import com.github.mczju.mczjuscription.roguelike.WanderingTraderService;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;

public final class TurnController {

    private final InscriptionMatch match;
    private TurnPhase phase = TurnPhase.DRAW;
    private int turnNumber = 1;
    private boolean drewFromRabbitPileThisTurn;
    /** 自由构牌：每回合主牌组只能抽一次。商店模式不使用。 */
    private boolean drewFromMainDeckThisTurn;

    public TurnController(InscriptionMatch match) {
        this.match = match;
    }

    public TurnPhase phase() {
        return phase;
    }

    public int turnNumber() {
        return turnNumber;
    }

    public boolean canDrawFromRabbitPile() {
        return phase == TurnPhase.DRAW && !drewFromRabbitPileThisTurn;
    }

    /** 商店：抽牌阶段可反复打开；自由构牌：每回合抽主牌组一次。 */
    public boolean canDrawFromMainDeck() {
        if (phase != TurnPhase.DRAW) {
            return false;
        }
        if (match.deckMode() == DeckMode.SHOP) {
            return true;
        }
        return !drewFromMainDeckThisTurn;
    }

    public void onDrawFromMainDeck() {
        drewFromMainDeckThisTurn = true;
        maybeLeaveDrawPhase();
    }

    public void onDrawFromRabbitPile() {
        drewFromRabbitPileThisTurn = true;
        maybeLeaveDrawPhase();
    }

    /** 商店购卡不结束抽牌阶段；仅兔子堆（或自由构牌抽牌）会进入出牌阶段。 */
    private void maybeLeaveDrawPhase() {
        if (match.deckMode() == DeckMode.SHOP) {
            if (drewFromRabbitPileThisTurn) {
                enterPhase(TurnPhase.PLAY);
            }
            return;
        }
        if (drewFromMainDeckThisTurn || drewFromRabbitPileThisTurn) {
            enterPhase(TurnPhase.PLAY);
        }
    }

    /** 商店模式：潜行+右键商店工具，不抽兔子也可进入出牌阶段。 */
    public void proceedToPlayFromDraw() {
        if (phase != TurnPhase.DRAW || match.deckMode() != DeckMode.SHOP) {
            return;
        }
        enterPhase(TurnPhase.PLAY);
    }

    public void ringBell() {
        if (phase != TurnPhase.PLAY) {
            return;
        }
        if (match.isCombatAnimating()) {
            match.feedback().actionBarWarn("<yellow>战斗进行中");
            return;
        }
        match.setCombatAnimating(true);
        match.breedingTracker().markCombatStart(match.board());
        enterPhase(TurnPhase.PLAYER_COMBAT);
        runCombatSequence();
    }

    private void runCombatSequence() {
        CombatResolver resolver = new CombatResolver(match);
        PreCombatResolver.resolve(match, MatchSide.PLAYER);
        match.setActiveCombatSide(MatchSide.PLAYER);
        resolver.resolveSideCombatAnimated(MatchSide.PLAYER, () -> {
            if (match.isMatchOver()) {
                finishCombatAnimation();
                return;
            }
            enterPhase(TurnPhase.ENEMY_PREP);
            PreviewAdvanceSequence.run(match, () -> {
                if (match.isMatchOver()) {
                    finishCombatAnimation();
                    return;
                }
                enterPhase(TurnPhase.ENEMY_COMBAT);
                PreCombatResolver.resolve(match, MatchSide.ENEMY);
                match.setActiveCombatSide(MatchSide.ENEMY);
                resolver.resolveSideCombatAnimated(MatchSide.ENEMY, () -> {
                    finishCombatAnimation();
                    if (!match.isMatchOver()) {
                        endTurn();
                    }
                });
            });
        });
    }

    private void finishCombatAnimation() {
        match.setCombatAnimating(false);
    }

    private void endTurn() {
        if (match.isMatchOver()) {
            finishCombatAnimation();
            return;
        }
        match.breedingTracker().resolveAfterCombat(match);
        planEnemyPreview();
        match.participant(match.actingSide()).currency().clearBloodOnTurnEnd();
        match.turnOwnership().endTurn();
        turnNumber++;
        drewFromRabbitPileThisTurn = false;
        drewFromMainDeckThisTurn = false;
        enterPhase(TurnPhase.DRAW);
    }

    public void enterPhase(TurnPhase next) {
        this.phase = next;
        if (match.isMatchOver()) {
            return;
        }
        if (next == TurnPhase.DRAW) {
            WanderingTraderService.onTraderTurnStart(match);
        } else if (next != TurnPhase.DRAW) {
            WanderingTraderService.despawnTrader(match);
        }
        match.feedback().announcePhase(next, turnNumber);
        match.syncHud();
    }

    public void planEnemyPreview() {
        match.planOpponentPreview();
    }
}
