package com.github.mczju.mczjuscription.shop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 单方对局内的当回合货架状态。 */
public final class ParticipantShopState {

  private final List<ShopOffer> rotating = new ArrayList<>();
  private final boolean[] rotatingSoldThisTurn = new boolean[ShopConfig.MAX_ROTATING_SLOTS];
  /** 本回合已从常驻货架购卡次数（上限 {@link #MAX_PERMANENT_PER_TURN}）。 */
  private int permanentPurchasesThisTurn;
  /** 本局是否已花腐肉解锁第 4 个刷新格。 */
  private boolean extraRotatingSlotUnlocked;

  public static final int MAX_PERMANENT_PER_TURN = 2;

  public void refreshFromConfig(ShopConfig config) {
    permanentPurchasesThisTurn = 0;
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

  public int permanentPurchasesThisTurn() {
    return permanentPurchasesThisTurn;
  }

  public boolean canBuyPermanentThisTurn() {
    return permanentPurchasesThisTurn < MAX_PERMANENT_PER_TURN;
  }

  /** @deprecated 使用 {@link #canBuyPermanentThisTurn()} */
  @Deprecated
  public boolean hasBoughtPermanentThisTurn() {
    return !canBuyPermanentThisTurn();
  }

  public void markPermanentPurchased() {
    permanentPurchasesThisTurn++;
  }
}
