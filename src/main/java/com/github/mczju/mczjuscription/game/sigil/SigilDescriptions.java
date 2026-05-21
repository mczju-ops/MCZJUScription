package com.github.mczju.mczjuscription.game.sigil;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 印记说明（设计器 / 流浪商人）。 */
public final class SigilDescriptions {

  /** 战斗层或 Registry 已接线的印记。 */
  private static final Set<SigilId> IMPLEMENTED =
      EnumSet.of(
          SigilId.AIR_STRIKE,
          SigilId.WATER_STRIKE,
          SigilId.HIGH_JUMP,
          SigilId.SPLIT_STRIKE,
          SigilId.TRI_STRIKE,
          SigilId.ALL_STRIKE,
          SigilId.BEAM,
          SigilId.DOUBLE_STRIKE,
          SigilId.VENOM_KILL,
          SigilId.HARD_SHELL,
          SigilId.FIRST_SHIELD,
          SigilId.INTIMIDATE,
          SigilId.SONAR,
          SigilId.STINKY,
          SigilId.STINKY_FAR,
          SigilId.WEB_WEAK,
          SigilId.SLOW,
          SigilId.SPIKY_ARMOR,
          SigilId.INK,
          SigilId.RIDING,
          SigilId.TAUNT_AURA,
          SigilId.BONE_ROYALTY,
          SigilId.COPY_ON_DEATH,
          SigilId.SELF_DESTRUCT,
          SigilId.SPLIT_SPAWN,
          SigilId.BREEDING,
          SigilId.FERMENT,
          SigilId.TRADE,
          SigilId.WATER_STORE,
          SigilId.FISH_BAIT,
          SigilId.FLEDGLING,
          SigilId.AFFLICTION,
          SigilId.QUALITY_SACRIFICE,
          SigilId.DEMON_OFFER,
          SigilId.ETERNAL_LIFE,
          SigilId.GUARD_DOG,
          SigilId.ENDER_SHIFT,
          SigilId.GUST_SHIFT,
          SigilId.WANDER,
          SigilId.RUSH_PUSH,
          SigilId.EVOKE_VEX,
          SigilId.STEAL_BONE,
          SigilId.SURPRISE_ENTRY,
          SigilId.SNIFF_STEAL,
          SigilId.COPY_ON_DEATH,
          SigilId.HISS,
          SigilId.SCORCH);

  /** 流浪商人随机印制池（排除亡语/秒杀类，避免印制破坏对局）。 */
  private static final Set<SigilId> TRADER_POOL = EnumSet.allOf(SigilId.class);

  static {
    TRADER_POOL.remove(SigilId.SELF_DESTRUCT);
    TRADER_POOL.remove(SigilId.VENOM_KILL);
    TRADER_POOL.remove(SigilId.SNIFF_STEAL);
  }

  private SigilDescriptions() {}

  public static List<SigilId> implementedForDesigner() {
    return EnumSet.allOf(SigilId.class).stream()
        .sorted(java.util.Comparator.comparing(Enum::name))
        .toList();
  }

  public static List<SigilId> traderImprintPool() {
    return TRADER_POOL.stream().sorted(java.util.Comparator.comparing(Enum::name)).toList();
  }

  public static boolean isImplemented(SigilId id) {
    return IMPLEMENTED.contains(id);
  }

  public static String description(SigilId id) {
    return switch (id) {
      case AIR_STRIKE -> "越过对方造物直击。";
      case WATER_STRIKE -> "对手回合潜水；潜水时对方造物可直击持牌人。";
      case HIGH_JUMP -> "拦截面前的空袭，必须以其为攻击目标。";
      case SPLIT_STRIKE -> "攻击正对面左右两列；空列直伤。";
      case TRI_STRIKE -> "攻击正对面左、中、右三列；空列直伤。";
      case ALL_STRIKE -> "攻击对面每一列；空列对该列造成直伤。";
      case BEAM -> "攻击时用献祭之剑右键敌方造物选定目标，再次右键确认。";
      case DOUBLE_STRIKE -> "对同一目标攻击两次。";
      case VENOM_KILL -> "造成伤害后目标立刻死亡。";
      case HARD_SHELL -> "单次最多受到 1 点伤害。";
      case FIRST_SHIELD -> "免疫上场后第一次受到的伤害。";
      case SPIKY_ARMOR -> "被攻击时反击 1 点伤害。";
      case INTIMIDATE -> "敌方攻击该造物时会放弃此次攻击。";
      case SONAR -> "可攻击带有水袭的单位。";
      case STINKY -> "相邻敌方力量 -1。";
      case STINKY_FAR -> "对面造物力量 -1。";
      case WEB_WEAK -> "受到攻击的造物力量 -1。";
      case SLOW -> "受到伤害的造物力量 -1。";
      case HISS -> "使对面【自爆】印记失效。";
      case ENDER_SHIFT -> "受伤后随机移动到其他空位。";
      case WANDER -> "攻击或回合结束后向随机空位移。";
      case RUSH_PUSH -> "移动时推挤相邻造物。";
      case GUST_SHIFT -> "攻击后随机移动到其他空位。";
      case RIDING -> "相邻友方力量 +1。";
      case TAUNT_AURA -> "场上所有造物（含敌方）力量 +1。";
      case SCORCH -> "攻击时额外 +1 伤害。";
      case BONE_ROYALTY -> "死亡时获得 4 骨币。";
      case COPY_ON_DEATH -> "死亡时将本卡复制品加入手牌。";
      case SELF_DESTRUCT -> "死亡时对相邻两格与面前格造成 10 点伤害。";
      case SPLIT_SPAWN -> "死亡时在相邻空位召唤两个小分身。";
      case EVOKE_VEX -> "出牌时在相邻空位召唤恼鬼。";
      case BREEDING -> "上场一回合后将本卡复制品加入手牌。";
      case FERMENT -> "在场一回合获得 2 腐肉。";
      case TRADE -> "在场一回合获得 1 骨币。";
      case WATER_STORE -> "每在场一回合生命 +1。";
      case FISH_BAIT -> "献祭时获得 1 张鱼干。";
      case QUALITY_SACRIFICE -> "献祭时获得 3 腐肉。";
      case DEMON_OFFER -> "献祭时获得 3 骨币。";
      case ETERNAL_LIFE -> "可无限次献祭。";
      case FLEDGLING -> "上场一回合后进化为指定生物。";
      case AFFLICTION -> "上场一回合后变为指定亡灵/下界形态。";
      case INK -> "对该造物造成伤害后，攻击者下回合无法攻击。";
      case STEAL_BONE -> "攻击时获得 1 骨币。";
      case SURPRISE_ENTRY -> "获得该卡时若场上有空位则免费召唤。";
      case SNIFF_STEAL -> "上场一回合后，从对面随机造物偷一个印记替换嗅探。";
      case GUARD_DOG -> "空位将遭攻击时移动到该格承担伤害。";
    };
  }

  public static List<String> loreLines(SigilId id) {
    return List.of("<dark_gray>" + description(id));
  }
}
