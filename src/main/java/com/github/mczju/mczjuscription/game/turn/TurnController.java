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

    /** 商店模式：购卡、领兔子、出牌、献祭均在 {@link TurnPhase#PLAY}（整备阶段）。 */
    public boolean isShopPlanningPhase() {
        return match.deckMode() == DeckMode.SHOP && phase == TurnPhase.PLAY;
    }

    public boolean hasDrawnFromRabbitPileThisTurn() {
        return drewFromRabbitPileThisTurn;
    }

    public boolean canDrawFromRabbitPile() {
        if (drewFromRabbitPileThisTurn) {
            return false;
        }
        if (match.deckMode() == DeckMode.SHOP) {
            return isShopPlanningPhase();
        }
        return phase == TurnPhase.DRAW;
    }

    /** 商店：整备阶段可反复打开；自由构牌：抽牌阶段每回合抽主牌组一次。 */
    public boolean canDrawFromMainDeck() {
        if (match.deckMode() == DeckMode.SHOP) {
            return isShopPlanningPhase();
        }
        if (phase != TurnPhase.DRAW) {
            return false;
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

    /** 自由构牌：抽牌结束后进入出牌；商店模式不切换阶段。 */
    private void maybeLeaveDrawPhase() {
        if (match.deckMode() == DeckMode.SHOP) {
            return;
        }
        if (drewFromMainDeckThisTurn || drewFromRabbitPileThisTurn) {
            enterPhase(TurnPhase.PLAY);
        }
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
        match.clearRoundResolvedDuringCombat();
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
            if (match.roundResolvedDuringCombat()) {
                finishCombatAnimation();
                endTurn();
                return;
            }
            enterPhase(TurnPhase.ENEMY_PREP);
            PreviewAdvanceSequence.run(match, () -> {
                if (match.isMatchOver()) {
                    finishCombatAnimation();
                    return;
                }
                if (match.roundResolvedDuringCombat()) {
                    finishCombatAnimation();
                    endTurn();
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
        enterPhase(planningPhaseForNewTurn());
    }

    private TurnPhase planningPhaseForNewTurn() {
        return match.deckMode() == DeckMode.SHOP ? TurnPhase.PLAY : TurnPhase.DRAW;
    }

    public void enterPhase(TurnPhase next) {
        this.phase = next;
        if (match.isMatchOver()) {
            return;
        }
        if (match.deckMode() == DeckMode.SHOP && next == TurnPhase.PLAY) {
            match.refreshShopOffers();
        }
        syncTraderPresence(next);
        match.feedback().announcePhase(next, turnNumber);
        match.syncHud();
    }

    public void planEnemyPreview() {
        match.planOpponentPreview();
    }

    /**
     * 商人回合（3、6、9…）在抽牌与出牌阶段均保留，便于献祭换腐肉后购买；
     * 进入战斗阶段或非商人回合时移除。
     */
    private void syncTraderPresence(TurnPhase next) {
        if (!WanderingTraderService.isTraderTurn(turnNumber)) {
            WanderingTraderService.despawnTrader(match);
            return;
        }
        if (next == TurnPhase.DRAW) {
            WanderingTraderService.onTraderTurnStart(match);
        } else if (next == TurnPhase.PLAY) {
            if (match.deckMode() == DeckMode.SHOP) {
                WanderingTraderService.onTraderTurnStart(match);
            } else {
                WanderingTraderService.ensureTraderPresent(match);
            }
        } else {
            WanderingTraderService.despawnTrader(match);
        }
    }
}
