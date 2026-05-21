package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.entity.EntityType;

/**
 * 内置卡牌默认数值（可被 cards.yml 覆盖）。
 *
 * @deprecated 正式玩法使用 {@code mob_*} 与 {@link CardCatalog}；仅作旧存档/机制 token 兼容。
 */
@Deprecated
public final class CardRegistry {

  private static final Map<CardId, CardDefinition> CARDS = new EnumMap<>(CardId.class);

  static {
    register(
        CardDefinition.builder(CardId.RABBIT)
            .displayName("兔子")
            .entity(EntityType.RABBIT)
            .stats(0, 1)
            .free()
            .build());

    register(
        CardDefinition.builder(CardId.BEE)
            .displayName("蜜蜂")
            .entity(EntityType.BEE)
            .stats(1, 1)
            .cost(CostType.BLOOD, 1)
            .sigil(SigilId.AIR_STRIKE)
            .build());

    register(
        CardDefinition.builder(CardId.ANT)
            .displayName("蚂蚁")
            .entity(EntityType.SILVERFISH)
            .stats(1, 1)
            .cost(CostType.BLOOD, 1)
            .build());

    register(
        CardDefinition.builder(CardId.WOLF_CUB)
            .displayName("狼崽")
            .entity(EntityType.WOLF)
            .stats(1, 1)
            .cost(CostType.BLOOD, 1)
            .sigil(SigilId.FLEDGLING)
            .build());

    register(
        CardDefinition.builder(CardId.WOLF)
            .displayName("狼")
            .entity(EntityType.WOLF)
            .stats(2, 2)
            .cost(CostType.BLOOD, 2)
            .build());

    register(
        CardDefinition.builder(CardId.GREAT_WOLF)
            .displayName("巨狼")
            .entity(EntityType.WOLF)
            .stats(3, 3)
            .free()
            .build());

    register(
        CardDefinition.builder(CardId.DAM_TOKEN)
            .displayName("堤坝(小)")
            .entity(EntityType.IRON_GOLEM)
            .stats(0, 2)
            .free()
            .build());

    register(
        CardDefinition.builder(CardId.PELT)
            .displayName("兽皮")
            .entity(EntityType.RABBIT)
            .stats(0, 2)
            .free()
            .build());

    register(
        CardDefinition.builder(CardId.BELL_TOKEN)
            .displayName("铃铛")
            .entity(EntityType.ALLAY)
            .stats(0, 1)
            .free()
            .build());

    register(
        CardDefinition.builder(CardId.TAIL)
            .displayName("尾巴")
            .entity(EntityType.SILVERFISH)
            .stats(0, 1)
            .free()
            .build());
  }

  private CardRegistry() {}

  public static void register(CardDefinition definition) {
    CARDS.put(definition.id(), definition);
  }

  public static CardDefinition get(CardId id) {
    CardDefinition def = CARDS.get(id);
    if (def == null) {
      throw new IllegalArgumentException("unknown card: " + id);
    }
    return def;
  }
}
