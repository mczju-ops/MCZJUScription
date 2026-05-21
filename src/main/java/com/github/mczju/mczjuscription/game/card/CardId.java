package com.github.mczju.mczjuscription.game.card;

import java.util.EnumSet;
import java.util.Set;

/**
 * 机制硬编码会发放或引用的内置卡牌 ID（共 10 种）。
 *
 * @deprecated 默认牌组与商店已改用 {@code mob_*}；保留供 token 与旧 yml 覆盖回退。
 */
@Deprecated
public enum CardId {
  /** 兔穴、商店开局、兔堆、敌人 AI */
  RABBIT,
  /** 内心之蜂（被攻击塞手牌）、敌人 AI */
  BEE,
  /** 蚁后（出牌塞手牌） */
  ANT,
  /** 铁兽夹（死亡塞手牌） */
  PELT,
  /** 筑坝师邻格 token，0/2 */
  DAM_TOKEN,
  /** 鸣钟人邻格 token */
  BELL_TOKEN,
  /** 断尾求生战前挡刀 token */
  TAIL,
  /** 幼雏成长原料；冰封关联；敌人 AI / 默认牌组 / 商店 */
  WOLF_CUB,
  /** 幼雏成长结果；敌人 AI / 默认牌组 / 商店 */
  WOLF,
  /** 冰封：场上 WOLF_CUB 死亡时替换 */
  GREAT_WOLF;

  private static final Set<CardId> DECK_BUILDER =
      EnumSet.of(RABBIT, BEE, ANT, WOLF_CUB, WOLF);

  /** 构牌界面可选（不含纯机制 token）。 */
  public static CardId[] deckBuilderPool() {
    return DECK_BUILDER.toArray(CardId[]::new);
  }

  /** 首次生成 shop.yml 时写入刷新池的候选。 */
  public static CardId[] shopSeedPool() {
    return new CardId[] {RABBIT, BEE, WOLF_CUB, WOLF, ANT};
  }
}
