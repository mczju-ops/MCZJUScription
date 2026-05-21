package com.github.mczju.mczjuscription.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 单方对局内的当回合货架状态。 */
public final class ParticipantShopState {

  private final List<ShopOffer> rotating = new ArrayList<>();
  private final boolean[] rotatingSoldThisTurn = new boolean[ShopConfig.MAX_ROTATING_SLOTS];
  private boolean permanentPurchasedThisTurn;
  /** 本局是否已花腐肉解锁第 4 个刷新格。 */
  private boolean extraRotatingSlotUnlocked;

  public void refreshFromConfig(ShopConfig config) {
    permanentPurchasedThisTurn = false;
    Arrays.fill(rotatingSoldThisTurn, false);
    rotating.clear();
    rotating.addAll(config.rollRotatingOffers());
    while (rotating.size() < ShopConfig.MAX_ROTATING_SLOTS) {
      rotating.add(ShopOffer.empty());
    }
  }

  public boolean isExtraRotatingSlotUnlocked() {
    return extraRotatingSlotUnlocked;
  }

  public void unlockExtraRotatingSlot() {
    extraRotatingSlotUnlocked = true;
  }

  public int activeRotatingSlotCount() {
    return extraRotatingSlotUnlocked
        ? ShopConfig.MAX_ROTATING_SLOTS
        : ShopConfig.BASE_ROTATING_SLOTS;
  }

  public ShopOffer rotatingOffer(int index) {
    if (index < 0 || index >= ShopConfig.MAX_ROTATING_SLOTS) {
      return ShopOffer.empty();
    }
    if (rotatingSoldThisTurn[index]) {
      return ShopOffer.empty();
    }
    return index < rotating.size() ? rotating.get(index) : ShopOffer.empty();
  }

  public boolean isRotatingSlotSold(int index) {
    return index >= 0
        && index < ShopConfig.MAX_ROTATING_SLOTS
        && rotatingSoldThisTurn[index];
  }

  public void markRotatingSold(int index) {
    if (index >= 0 && index < ShopConfig.MAX_ROTATING_SLOTS) {
      rotatingSoldThisTurn[index] = true;
    }
  }

  public boolean hasBoughtPermanentThisTurn() {
    return permanentPurchasedThisTurn;
  }

  public void markPermanentPurchased() {
    permanentPurchasedThisTurn = true;
  }
}
