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
                .sigil(SigilId.AIR_STRIKE)
                .build());

        register(CardDefinition.builder(CardId.ANT)
                .displayName("蚂蚁")
                .entity(EntityType.SILVERFISH)
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

        register(CardDefinition.builder(CardId.DAM_TOKEN)
                .displayName("堤坝")
                .entity(EntityType.IRON_GOLEM)
                .stats(0, 2)
                .free()
                .build());

        register(CardDefinition.builder(CardId.PELT)
                .displayName("兽皮")
                .entity(EntityType.RABBIT)
                .stats(0, 2)
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

        register(CardDefinition.builder(CardId.HARE)
                .displayName("兔")
                .entity(EntityType.RABBIT)
                .stats(0, 1)
                .free()
                .sigil(SigilId.RABBIT_HOLE)
                .build());

        register(CardDefinition.builder(CardId.GUARDIAN)
                .displayName("守护者")
                .entity(EntityType.WOLF)
                .stats(1, 2)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.GUARD_DOG)
                .build());

        register(CardDefinition.builder(CardId.MOLE)
                .displayName("钻地龙")
                .entity(EntityType.SILVERFISH)
                .stats(0, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.WHACK_A_MOLE)
                .build());

        register(CardDefinition.builder(CardId.GECKO)
                .displayName("断尾蜥")
                .entity(EntityType.SILVERFISH)
                .stats(1, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.TAIL_ON_HIT)
                .build());

        register(CardDefinition.builder(CardId.SKUNK)
                .displayName("臭鼬")
                .entity(EntityType.SILVERFISH)
                .stats(1, 2)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.STINKY)
                .build());

        register(CardDefinition.builder(CardId.COYOTE)
                .displayName("郊狼")
                .entity(EntityType.WOLF)
                .stats(2, 1)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.RUSH_LEFT)
                .sigil(SigilId.RUSH_PUSH)
                .build());

        register(CardDefinition.builder(CardId.FROG)
                .displayName("青蛙")
                .entity(EntityType.FROG)
                .stats(1, 2)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.WATER_STRIKE)
                .build());

        register(CardDefinition.builder(CardId.BAT)
                .displayName("蝙蝠")
                .entity(EntityType.BAT)
                .stats(1, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.HIGH_JUMP)
                .build());

        register(CardDefinition.builder(CardId.SHREW)
                .displayName("鼩鼱")
                .entity(EntityType.SILVERFISH)
                .stats(0, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.PREVENT_ATTACK)
                .build());

        register(CardDefinition.builder(CardId.MANTIS)
                .displayName("螳螂")
                .entity(EntityType.SILVERFISH)
                .stats(1, 1)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.SPLIT_STRIKE)
                .build());

        register(CardDefinition.builder(CardId.BEAVER)
                .displayName("河狸")
                .entity(EntityType.SILVERFISH)
                .stats(1, 2)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.TRI_STRIKE)
                .build());

        register(CardDefinition.builder(CardId.GRIZZLY)
                .displayName("灰熊")
                .entity(EntityType.POLAR_BEAR)
                .stats(2, 3)
                .cost(CostType.BLOOD, 3)
                .sigil(SigilId.ALL_STRIKE)
                .build());

        register(CardDefinition.builder(CardId.GOAT)
                .displayName("山羊")
                .entity(EntityType.GOAT)
                .stats(0, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.ETERNAL_LIFE)
                .sigil(SigilId.QUALITY_SACRIFICE)
                .build());

        register(CardDefinition.builder(CardId.COCKROACH)
                .displayName("蟑螂")
                .entity(EntityType.SILVERFISH)
                .stats(1, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.COPY_ON_DEATH)
                .build());

        register(CardDefinition.builder(CardId.MAGPIE)
                .displayName("喜鹊")
                .entity(EntityType.PARROT)
                .stats(1, 2)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.COPY_ON_PLAY)
                .build());

        register(CardDefinition.builder(CardId.ADDER)
                .displayName("蝰蛇")
                .entity(EntityType.SILVERFISH)
                .stats(1, 1)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.TOUCH_OF_DEATH)
                .build());

        register(CardDefinition.builder(CardId.BULL)
                .displayName("公牛")
                .entity(EntityType.COW)
                .stats(2, 3)
                .cost(CostType.BLOOD, 3)
                .sigil(SigilId.LEADER_POWER)
                .build());

        register(CardDefinition.builder(CardId.PACK_RAT)
                .displayName("囤鼠")
                .entity(EntityType.RABBIT)
                .stats(1, 1)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.TUTOR)
                .build());

        register(CardDefinition.builder(CardId.GHOUL)
                .displayName("食尸鬼")
                .entity(EntityType.ZOMBIE)
                .stats(2, 2)
                .cost(CostType.BONES, 3)
                .sigil(SigilId.CORPSE_EATER)
                .build());

        register(CardDefinition.builder(CardId.AMALGAM)
                .displayName("无形之物")
                .entity(EntityType.SLIME)
                .stats(2, 2)
                .cost(CostType.BLOOD, 3)
                .sigil(SigilId.RANDOM_SIGIL)
                .build());

        register(CardDefinition.builder(CardId.CAT)
                .displayName("猫")
                .entity(EntityType.CAT)
                .stats(0, 1)
                .cost(CostType.BLOOD, 1)
                .sigil(SigilId.SPIKY_ARMOR)
                .sigil(SigilId.BEE_STING)
                .build());

        register(CardDefinition.builder(CardId.STUMP)
                .displayName("筑坝师")
                .entity(EntityType.IRON_GOLEM)
                .stats(0, 3)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.DAM_BUILDER)
                .build());

        register(CardDefinition.builder(CardId.BELL_SHEEP)
                .displayName("鸣钟羊")
                .entity(EntityType.SHEEP)
                .stats(0, 2)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.BELL_RINGER)
                .build());

        register(CardDefinition.builder(CardId.ROCK)
                .displayName("磐石")
                .entity(EntityType.IRON_GOLEM)
                .stats(0, 4)
                .cost(CostType.BLOOD, 2)
                .sigil(SigilId.ROCK_BODY)
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
