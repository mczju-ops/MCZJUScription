package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.ArrayList;
import java.util.List;

/** 流浪商人：两卡融合。 */
public final class CardFusion {

  private CardFusion() {}

  /**
   * 融合 A 与 B：名称拼接、属性与费用相加、印记合并（最多 3），模型为 A 骑乘 B。
   */
  public static CardTemplate fuse(CardTemplate a, CardTemplate b) {
    String id = CardCatalog.newCustomId();
    CardTemplate fused = new CardTemplate(id);
    fused.setDisplayName(a.displayName() + b.displayName());
    fused.setEntityType(a.entityType());
    fused.setMountEntityType(b.entityType());
    fused.setPower(a.power() + b.power());
    fused.setHealth(a.health() + b.health());
    fused.setCost(a.cost() + b.cost());
    fused.setSacrificeValue(a.sacrificeValue() + b.sacrificeValue());
    if (a.costType() == b.costType()) {
      fused.setCostType(a.costType());
    } else if (a.costType() == CostType.FREE) {
      fused.setCostType(b.costType());
    } else if (b.costType() == CostType.FREE) {
      fused.setCostType(a.costType());
    } else {
      fused.setCostType(CostType.BLOOD);
    }
    List<SigilId> sigils = new ArrayList<>();
    sigils.addAll(a.sigils());
    sigils.addAll(b.sigils());
    fused.setSigils(SigilRules.merge(sigils, List.of()));
    fused.setBuiltin(false);
    CardCatalog.saveRuntime(fused);
    return fused;
  }
}
