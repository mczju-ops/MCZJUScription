package com.github.mczju.mczjuscription.item;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.CostType;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class InscriptionCardItem extends MGCItem {

  private final String templateId;
  private final String itemId;

  public InscriptionCardItem(String templateId) {
    this.templateId = templateId;
    this.itemId = InscriptionItems.cardItemId(templateId);
  }

  @Override
  public String getId() {
    return itemId;
  }

  public String templateId() {
    return templateId;
  }

  @Override
  protected ItemStack createRawItem() {
    CardTemplate def = CardCatalog.require(templateId);
    List<String> lore = new ArrayList<>();
    lore.add("<gray>力量 %d  |  生命 %d".formatted(def.power(), def.health()));
    lore.add(costLine(def));
    if (!def.sigils().isEmpty()) {
      lore.add("<dark_purple>印记: <light_purple>" + def.sigilsDisplay());
    }
    lore.add("<dark_gray>ID: " + templateId);
    lore.add("<dark_gray>丢弃到己方槽位方块上召唤");
    return ItemBuilder.of(cardMaterial(def))
        .customName("<white>" + def.displayName())
        .lore(lore)
        .maxStackSize(16)
        .build();
  }

  private static String costLine(CardTemplate def) {
    return switch (def.costType()) {
      case FREE -> "<green>花费：免费";
      case BLOOD -> "<red>花费：腐肉 ×%d".formatted(def.cost());
      case BONES -> "<gold>花费：骨币 ×%d".formatted(def.cost());
      case FISH -> "<aqua>花费：鱼干 ×%d".formatted(def.cost());
    };
  }

  private static Material cardMaterial(CardTemplate def) {
    if ("mob_fish_dried".equals(def.id())) {
      return Material.COD;
    }
    return def.spawnEggMaterial();
  }
}
