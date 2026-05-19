package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchSetup;

/** 通关难度标量（首版启发式，后续可接章节/诅咒表）。 */
public final class InscriptionRunScoring {

    private InscriptionRunScoring() {}

    public static int computeRunDifficulty(MatchSetup setup, InscriptionPlayerData data) {
        int score = 1;
        if (setup.deckMode() == DeckMode.SHOP) {
            score += 1;
        }
        if (setup.variant().isDuel()) {
            score += 2;
        }
        String curse = data.selectedCurse;
        if (curse != null && !curse.isBlank() && !"none".equalsIgnoreCase(curse)) {
            score += 1;
        }
        return score;
    }

    public static void recordClear(InscriptionPlayerData data, int runDifficulty) {
        int best = data.bestClearDifficulty == null ? 0 : data.bestClearDifficulty;
        int clears = data.clearCountAtBestDifficulty == null ? 0 : data.clearCountAtBestDifficulty;
        if (runDifficulty > best) {
            data.bestClearDifficulty = runDifficulty;
            data.clearCountAtBestDifficulty = 1;
        } else if (runDifficulty == best) {
            data.clearCountAtBestDifficulty = clears + 1;
        }
        data.setModified(true);
    }
}
