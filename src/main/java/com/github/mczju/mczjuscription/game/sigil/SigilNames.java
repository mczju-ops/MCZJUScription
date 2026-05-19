package com.github.mczju.mczjuscription.game.sigil;

import java.util.Set;
import java.util.stream.Collectors;

public final class SigilNames {

  private SigilNames() {}

  public static String display(SigilId id) {
    return switch (id) {
      case RABBIT_HOLE -> "兔穴";
      case BEE_STING -> "内心之蜂";
      case COPY_ON_PLAY -> "丰产之巢";
      case DAM_BUILDER -> "筑坝师";
      case BELL_RINGER -> "鸣钟人";
      case ANT_QUEEN -> "蚁后";
      case COPY_ON_DEATH -> "不死之虫";
      case ICY_ENTOMB -> "冰封禁锢";
      case STEEL_TRAP -> "铁兽夹";
      case SPIKY_ARMOR -> "尖刺铠甲";
      case AIR_STRIKE -> "空袭";
      case WATER_STRIKE -> "水袭";
      case HIGH_JUMP -> "高跳";
      case STINKY -> "臭臭";
      case ROCK_BODY -> "磐石之身";
      case TOUCH_OF_DEATH -> "死神之触";
      case BONE_ROYALTY -> "骨皇";
      case ETERNAL_LIFE -> "生生不息";
      case QUALITY_SACRIFICE -> "优质祭品";
      case PREVENT_ATTACK -> "厌恶情绪";
      case SPLIT_STRIKE -> "兵分两路";
      case TRI_STRIKE -> "兵分三路";
      case ALL_STRIKE -> "全向打击";
      case LEADER_POWER -> "领袖力量";
      case RUSH_LEFT -> "左冲";
      case RUSH_RIGHT -> "右冲";
      case RUSH_PUSH -> "蛮力冲撞";
      case GUARD_DOG -> "守护者";
      case WHACK_A_MOLE -> "钻地龙";
      case TAIL_ON_HIT -> "断尾求生";
      case BREEDING -> "繁殖";
      case FLEDGLING -> "幼雏";
      case TUTOR -> "囤积狂";
      case CORPSE_EATER -> "食尸鬼";
      case ORBIT -> "潮汐锁定";
      case RANDOM_SIGIL -> "无形之物";
      case ITEM_VENDOR -> "道具商";
    };
  }

  public static String join(Set<SigilId> sigils) {
    if (sigils.isEmpty()) return "无";
    return sigils.stream().map(SigilNames::display).collect(Collectors.joining("、"));
  }
}
