package com.github.mczju.mczjuscription.game.deck;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 对局开始时向玩家发放初始手牌（不占用回合抽牌次数）。 */
public final class OpeningHandDealer {

  private static final int FREE_BUILD_OPENING = 4;

  private OpeningHandDealer() {}

  public static void deal(InscriptionMatch match) {
    for (ParticipantState human : match.humanParticipants()) {
      if (match.deckMode() == DeckMode.SHOP) {
        dealShopOpening(match, human.side());
      } else {
        dealFreeBuildOpening(match, human.side());
      }
    }
  }

  private static void dealFreeBuildOpening(InscriptionMatch match, MatchSide side) {
    ParticipantState state = match.participant(side);
    List<String> drawn = new ArrayList<>();
    while (drawn.size() < FREE_BUILD_OPENING && !state.mainDeck().isEmpty()) {
      drawn.add(state.mainDeck().removeFirst());
    }
    for (String templateId : drawn) {
      match.grantCardToHandSilent(side, templateId);
    }
  }

  private static void dealShopOpening(InscriptionMatch match, MatchSide side) {
    for (int i = 0; i < 3; i++) {
      match.grantCardToHandSilent(side, "mob_rabbit");
    }
    String bonus = randomShopCreature();
    match.grantCardToHandSilent(side, bonus);
  }

  private static String randomShopCreature() {
    String rolled = ShopConfigStorage.get().randomRotatingTemplateId();
    if (rolled != null && !"mob_rabbit".equalsIgnoreCase(rolled)) {
      return rolled;
    }
    return "mob_wolf";
  }
}
