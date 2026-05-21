package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import java.util.Map;

/** 【稚雏】/【折磨】：在场一回合后按 cards.yml {@code evolvesTo} 替换场面造物。 */
public final class EvolutionResolver {

  private static final Map<String, String> DEFAULT_EVOLVES =
      Map.of(
          "mob_wolf_cub", "mob_wolf",
          "mob_tadpole", "mob_frog",
          "mob_piglin", "mob_zombified_piglin",
          "mob_hoglin", "mob_zoglin");

  private EvolutionResolver() {}

  public static void mature(InscriptionMatch match, BoardCreature creature) {
    BoardSlot slot = creature.slot();
    if (slot == null || creature.isDead()) {
      return;
    }

    String target = CardCatalog.get(creature.templateId()).evolvesTo();
    if (target == null || target.isBlank()) {
      target = DEFAULT_EVOLVES.get(creature.templateId());
    }
    if (target != null && CardCatalog.exists(target)) {
      match.replaceWith(slot, target, creature.owner());
      return;
    }

    if (creature.hasSigil(SigilId.FLEDGLING) && !creature.hasMaturedFromFledgling()) {
      creature.applyElderForm();
      com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(creature);
    }
  }
}
