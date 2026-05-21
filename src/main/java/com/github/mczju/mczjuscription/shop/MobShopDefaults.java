package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import java.util.List;

/** 从 mob 卡池生成 shop.yml 默认刷新池。 */
public final class MobShopDefaults {

  private MobShopDefaults() {}

  public static void seedRotatingPool(ShopConfig config) {
    List<String> pool = CardCatalog.deckBuilderPool();
    for (String id : pool) {
      config.rotatingPool().add(new ShopPoolEntry(id, bonePrice(id), weight(id)));
    }
  }

  private static int bonePrice(String templateId) {
    return switch (templateId) {
      case "mob_rabbit" -> 0;
      case "mob_bee", "mob_wolf_cub", "mob_tadpole" -> 2;
      case "mob_wolf", "mob_frog" -> 3;
      case "mob_piglin_brute", "mob_vindicator" -> 5;
      case "mob_warden" -> 6;
      default -> 2;
    };
  }

  private static int weight(String templateId) {
    CardTemplate t = CardCatalog.get(templateId);
    if (t == null) return 10;
    int p = t.power() + t.health();
    if (p >= 8) return 4;
    if (p >= 5) return 7;
    return 10;
  }
}
