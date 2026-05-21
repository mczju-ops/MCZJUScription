package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import java.util.Map;

/** 【分裂】：死亡时在相邻空位召唤小分身。 */
public final class SplitSpawnDeath {

  private static final Map<String, String> TOKEN_BY_PARENT =
      Map.of(
          "mob_slime", "mob_slime_small",
          "mob_magma_cube", "mob_magma_cube_small");

  private SplitSpawnDeath() {}

  public static void spawn(InscriptionMatch match, BoardCreature parent) {
    if (!parent.hasSigil(SigilId.SPLIT_SPAWN)) return;
    String token = CardCatalog.get(parent.templateId()).evolvesTo();
    if (token == null || token.isBlank()) {
      token = TOKEN_BY_PARENT.get(parent.templateId());
    }
    if (token == null || !CardCatalog.exists(token)) return;
    BoardTokens.spawnOnAdjacentEmpty(match, parent, token);
  }
}
