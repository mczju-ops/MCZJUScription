package com.github.mczju.mczjuscription.game.sigil;

import java.util.Set;
import java.util.stream.Collectors;

public final class SigilNames {

    private SigilNames() {}

    public static String display(SigilId id) {
        return switch (id) {
            case RABBIT_HOLE -> "兔穴";
            case SPIKY_ARMOR -> "尖刺";
            case AIR_STRIKE -> "空袭";
            case WATER_STRIKE -> "水袭";
            case HIGH_JUMP -> "高跳";
            case STINKY -> "臭臭";
            case ROCK_BODY -> "磐石";
            case TOUCH_OF_DEATH -> "死神之触";
            case BONE_ROYALTY -> "骨皇";
            case ETERNAL_LIFE -> "生生不息";
            case QUALITY_SACRIFICE -> "优质祭品";
            case ICY_ENTOMB -> "冰封";
            case FLEDGLING -> "幼雏";
            case LEADER_POWER -> "领袖";
            case RUSH_LEFT -> "左冲";
            case RUSH_RIGHT -> "右冲";
            case BREEDING -> "繁殖";
        };
    }

    public static String join(Set<SigilId> sigils) {
        if (sigils.isEmpty()) return "无";
        return sigils.stream().map(SigilNames::display).collect(Collectors.joining("、"));
    }
}
