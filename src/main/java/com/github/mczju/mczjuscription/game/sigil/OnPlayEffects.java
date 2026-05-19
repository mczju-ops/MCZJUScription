package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

/** 出牌时触发的产牌 / 邻格 token。 */
public final class OnPlayEffects {

  private OnPlayEffects() {}

  public static void apply(InscriptionMatch match, BoardCreature source, SigilId sigil) {
    switch (sigil) {
      case RABBIT_HOLE -> match.grantCardToHandSilent(source.owner(), CardId.RABBIT.name());
      case COPY_ON_PLAY -> match.grantCardToHandSilent(source.owner(), source.templateId());
      case ANT_QUEEN -> match.grantCardToHandSilent(source.owner(), CardId.ANT.name());
      case DAM_BUILDER -> BoardTokens.spawnOnAdjacentEmpty(match, source, CardId.DAM_TOKEN.name());
      case BELL_RINGER -> BoardTokens.spawnOnAdjacentEmpty(match, source, CardId.BELL_TOKEN.name());
      default -> {}
    }
  }
}
