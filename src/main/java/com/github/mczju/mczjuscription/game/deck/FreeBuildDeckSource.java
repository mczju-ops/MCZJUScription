package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import java.util.List;

public final class FreeBuildDeckSource implements DeckSource {

  private final List<String> initialDeck;

  public FreeBuildDeckSource(List<String> initialDeck) {
    this.initialDeck = List.copyOf(initialDeck);
  }

  @Override
  public com.github.mczju.mczjuscription.game.session.DeckMode mode() {
    return com.github.mczju.mczjuscription.game.session.DeckMode.FREE_BUILD;
  }

  @Override
  public String mainDrawItemHint() {
    return "<gray>右键从主牌组抽一张牌";
  }

  @Override
  public void initializeDeck(ParticipantState state, InscriptionMatch match) {
    state.mainDeck().clear();
    state.mainDeck().addAll(initialDeck);
  }

  @Override
  public void performMainDrawAction(InscriptionMatch match, MatchSide side) {
    ParticipantState state = match.participant(side);
    if (state.mainDeck().isEmpty()) {
      match.feedback().actionBarWarn("<yellow>主牌组已空");
      return;
    }
    String drawn = state.mainDeck().pollFirst();
    match.grantCardToHand(side, drawn);
    match.completeMainDraw(side);
  }
}
