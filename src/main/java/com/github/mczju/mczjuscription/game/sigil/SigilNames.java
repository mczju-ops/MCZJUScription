package com.github.mczju.mczjuscription.game.sigil;

import java.util.Set;
import java.util.stream.Collectors;

public final class SigilNames {

  private SigilNames() {}

  public static String display(SigilId id) {
    return switch (id) {
      case AIR_STRIKE -> "空袭";
      case WATER_STRIKE -> "水袭";
      case HIGH_JUMP -> "高跳";
      case SPLIT_STRIKE -> "兵分两路";
      case TRI_STRIKE -> "兵分三路";
      case ALL_STRIKE -> "全向攻击";
      case BEAM -> "射线";
      case DOUBLE_STRIKE -> "追击";
      case VENOM_KILL -> "剧毒";
      case HARD_SHELL -> "坚壳";
      case FIRST_SHIELD -> "护盾";
      case SPIKY_ARMOR -> "尖刺铠甲";
      case INTIMIDATE -> "震慑";
      case SONAR -> "声呐";
      case STINKY -> "臭臭";
      case STINKY_FAR -> "臭臭";
      case WEB_WEAK -> "蛛网";
      case SLOW -> "迟缓";
      case HISS -> "哈气";
      case ENDER_SHIFT -> "穿梭";
      case WANDER -> "游荡";
      case RUSH_PUSH -> "蛮力";
      case GUST_SHIFT -> "蓄风";
      case RIDING -> "骑乘";
      case TAUNT_AURA -> "嘲讽";
      case SCORCH -> "炽热";
      case BONE_ROYALTY -> "骨皇";
      case COPY_ON_DEATH -> "不死之虫";
      case SELF_DESTRUCT -> "自爆";
      case SPLIT_SPAWN -> "分裂";
      case EVOKE_VEX -> "唤魔";
      case BREEDING -> "繁殖";
      case FERMENT -> "发酵";
      case TRADE -> "交易";
      case WATER_STORE -> "储水";
      case FISH_BAIT -> "鱼饵";
      case QUALITY_SACRIFICE -> "优质祭品";
      case DEMON_OFFER -> "恶魔";
      case ETERNAL_LIFE -> "生生不息";
      case FLEDGLING -> "稚雏";
      case AFFLICTION -> "折磨";
      case INK -> "墨水";
      case STEAL_BONE -> "盗物";
      case SURPRISE_ENTRY -> "意外";
      case SNIFF_STEAL -> "嗅探";
      case GUARD_DOG -> "守卫者";
      case ABUNDANCE -> "丰饶";
    };
  }

  public static String join(Set<SigilId> sigils) {
    if (sigils.isEmpty()) return "无";
    return sigils.stream().map(SigilNames::display).collect(Collectors.joining("、"));
  }
}
