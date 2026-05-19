package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.sigil.SigilId;
import org.bukkit.entity.EntityType;

import java.util.EnumMap;
import java.util.Map;

public final class CardRegistry {

    private static final Map<CardId, CardDefinition> CARDS = new EnumMap<>(CardId.class);

    static {
        register(CardDefinition.builder(CardId.WOLF)
                .displayName("狼")
                .entity(EntityType.WOLF)
                .stats(2, 2)
                .cost(CostType.BLOOD, 2)
                .build());

        register(CardDefinition.builder(CardId.RABBIT)
                .displayName("兔子")
                .entity(EntityType.RABBIT)
                .stats(0, 1)
                .free()
                .build());

        register(CardDefinition.builder(CardId.BEE)
                .displayName("蜜蜂")
                .entity(EntityType.BEE)
                .stats(1, 1)
                .cost(CostType.BLOOD, 1)
                .build());

        register(CardDefinition.builder(CardId.CHICKEN)
                .displayName("鸡")
                .entity(EntityType.CHICKEN)
                .stats(0, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.BREEDING)
                .build());

        register(CardDefinition.builder(CardId.WOLF_CUB)
                .displayName("狼崽")
                .entity(EntityType.WOLF)
                .stats(1, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.FLEDGLING)
                .build());

        register(CardDefinition.builder(CardId.GREAT_WOLF)
                .displayName("巨狼")
                .entity(EntityType.WOLF)
                .stats(3, 3)
                .free()
                .build());

        register(CardDefinition.builder(CardId.BONE_LORD)
                .displayName("骨皇")
                .entity(EntityType.WITHER_SKELETON)
                .stats(3, 4)
                .cost(CostType.BONES, 4)
                .sigil(SigilId.BONE_ROYALTY)
                .build());

        register(CardDefinition.builder(CardId.DAM)
                .displayName("堤坝")
                .entity(EntityType.IRON_GOLEM)
                .stats(0, 5)
                .free()
                .build());

        register(CardDefinition.builder(CardId.BELL_TOKEN)
                .displayName("铃铛")
                .entity(EntityType.ALLAY)
                .stats(0, 1)
                .free()
                .build());

        register(CardDefinition.builder(CardId.TAIL)
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
        if (def == null) throw new IllegalArgumentException("unknown card: " + id);
        return def;
    }
}
