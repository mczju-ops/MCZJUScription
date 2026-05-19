package com.github.mczju.mczjuscription.game.session;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.ai.AiOpponentController;
import com.github.mczju.mczjuscription.game.match.LifeSystem;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 构建 {@link MatchSetup}，覆盖 2×2 全部组合。 */
public final class MatchSetupFactory {

    private MatchSetupFactory() {}

    public static MatchSetup create(PlayVariant variant, List<PlayerExt> players, InscriptionGameRoom room) {
        int playerCandles = room.playerCandles != null ? room.playerCandles : LifeSystem.DEFAULT_PLAYER_CANDLES;
        int enemyCandles = room.enemyCandles != null ? room.enemyCandles : LifeSystem.DEFAULT_ENEMY_CANDLES;

        return switch (variant.matchMode()) {
            case SOLO_PVE -> solo(players.getFirst(), playerCandles, enemyCandles, variant.deckMode());
            case DUEL_PVP -> {
                if (players.size() < 2) {
                    throw new IllegalStateException("双人对战需要 2 名玩家");
                }
                int candles = playerCandles;
                yield duel(players.get(0), players.get(1), candles, variant.deckMode());
            }
        };
    }

    public static MatchSetup solo(PlayerExt human, int playerCandles, int enemyCandles, DeckMode deckMode) {
        Map<MatchSide, Integer> candles = new EnumMap<>(MatchSide.class);
        candles.put(MatchSide.PLAYER, playerCandles);
        candles.put(MatchSide.ENEMY, enemyCandles);
        return new MatchSetup(
                new PlayVariant(MatchMode.SOLO_PVE, deckMode),
                List.of(
                        MatchParticipant.human(MatchSide.PLAYER, human),
                        MatchParticipant.ai(MatchSide.ENEMY)
                ),
                candles,
                new AiOpponentController()
        );
    }

    public static MatchSetup duel(PlayerExt seatPlayer, PlayerExt seatEnemy, int candlesEach, DeckMode deckMode) {
        Map<MatchSide, Integer> candles = new EnumMap<>(MatchSide.class);
        candles.put(MatchSide.PLAYER, candlesEach);
        candles.put(MatchSide.ENEMY, candlesEach);
        return new MatchSetup(
                new PlayVariant(MatchMode.DUEL_PVP, deckMode),
                List.of(
                        MatchParticipant.human(MatchSide.PLAYER, seatPlayer),
                        MatchParticipant.human(MatchSide.ENEMY, seatEnemy)
                ),
                candles,
                null
        );
    }

    /** @deprecated 使用 {@link #create(PlayVariant, List, InscriptionGameRoom)} */
    public static MatchSetup solo(PlayerExt human, int playerCandles, int enemyCandles) {
        return solo(human, playerCandles, enemyCandles, DeckMode.FREE_BUILD);
    }

    /** @deprecated 使用 {@link #create(PlayVariant, List, InscriptionGameRoom)} */
    public static MatchSetup duel(PlayerExt seatPlayer, PlayerExt seatEnemy, int candlesEach) {
        return duel(seatPlayer, seatEnemy, candlesEach, DeckMode.FREE_BUILD);
    }
}
