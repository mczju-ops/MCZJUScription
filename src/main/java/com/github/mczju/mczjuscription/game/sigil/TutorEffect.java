package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.menu.TutorPickMenu;

/** 囤积狂：打开选牌菜单。 */
public final class TutorEffect {

  private TutorEffect() {}

  public static void open(InscriptionMatch match, BoardCreature source) {
    match
        .participant(source.owner())
        .player()
        .ifPresent(
            ext -> new TutorPickMenu(ext.player(), match, source.owner()).open());
  }
}
