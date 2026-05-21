package com.github.mczju.mczjuscription.shop;

import java.util.ArrayList;
import java.util.List;

/** 全局商店布局与奖池（管理员配置，持久化至 shop.yml）。 */
public final class ShopConfig {

  public static final int PERMANENT_SLOTS = 4;
  /** 开局默认可用的刷新格数量。 */
  public static final int BASE_ROTATING_SLOTS = 3;
  /** 解锁第 4 格后的刷新格总数。 */
  public static final int MAX_ROTATING_SLOTS = 4;
  public static final int DEFAULT_EXTRA_SLOT_UNLOCK_BLOOD = 5;

  private final List<ShopOffer> permanent = new ArrayList<>();
  private final List<ShopPoolEntry> rotatingPool = new ArrayList<>();
  private int extraSlotUnlockBlood = DEFAULT_EXTRA_SLOT_UNLOCK_BLOOD;

  public ShopConfig() {
    for (int i = 0; i < PERMANENT_SLOTS; i++) {
      permanent.add(ShopOffer.empty());
    }
  }

  public List<ShopOffer> permanentSlots() {
    return permanent;
  }

  public ShopOffer permanent(int index) {
    return permanent.get(index);
  }

  public void setPermanent(int index, ShopOffer offer) {
    permanent.set(index, offer != null ? offer : ShopOffer.empty());
  }

  public List<ShopPoolEntry> rotatingPool() {
    return rotatingPool;
  }

  public int extraSlotUnlockBlood() {
    return extraSlotUnlockBlood;
  }

  public void setExtraSlotUnlockBlood(int cost) {
    this.extraSlotUnlockBlood = Math.max(0, cost);
  }

  public List<ShopOffer> rollRotatingOffers() {
    return ShopRoller.roll(rotatingPool, MAX_ROTATING_SLOTS);
  }

  public String randomRotatingTemplateId() {
    ShopOffer offer = ShopRoller.rollOne(rotatingPool);
    return offer.isEmpty() ? null : offer.templateId();
  }
}
