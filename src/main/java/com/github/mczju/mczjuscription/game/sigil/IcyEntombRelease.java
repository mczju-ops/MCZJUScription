package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

import java.util.Map;

/** 冰封禁锢：阵亡时槽位由映射表中的造物取代（与幼雏成长线独立）。 */
public final class IcyEntombRelease {

  private static final Map<String, String> RELEASE_MAP =
      Map.of(CardId.WOLF_CUB.name(), CardId.GREAT_WOLF.name());

  private IcyEntombRelease() {}

  public static void release(InscriptionMatch match, BoardCreature creature) {
    if (!creature.hasSigil(SigilId.ICY_ENTOMB)) return;
    String replacement = RELEASE_MAP.get(creature.templateId());
    if (replacement == null) return;
    BoardSlot slot = creature.slot();
    if (slot == null) return;
    match.replaceWith(slot, replacement, creature.owner());
  }
}
