package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.menu.InscriptionShopMenu;

/** 商店：骨币购卡，抽牌阶段可反复打开。 */
public final class SimpleShopPresenter implements ShopPresenter {

  @Override
  public void open(InscriptionMatch match, MatchSide buyerSide) {
    if (!match.turn().canDrawFromMainDeck()) {
      return;
    }
    match.participant(buyerSide).player().ifPresent(ext ->
        new InscriptionShopMenu(ext.player(), match, buyerSide, this).open());
  }

  @Override
  public boolean purchase(
      InscriptionMatch match,
      MatchSide buyerSide,
      ShopOffer offer,
      ShopPurchaseLane lane,
      int rotatingSlotIndex) {
    if (offer == null || offer.isEmpty()) {
      return false;
    }
    ParticipantShopState shop = match.shopState(buyerSide);

    if (lane == ShopPurchaseLane.PERMANENT) {
      if (shop.hasBoughtPermanentThisTurn()) {
        match.feedback().actionBarWarn("<yellow>本回合已从常驻货架购卡，下回合再来。");
        return false;
      }
    } else {
      if (rotatingSlotIndex < 0 || rotatingSlotIndex >= ShopConfig.MAX_ROTATING_SLOTS) {
        return false;
      }
      if (rotatingSlotIndex >= shop.activeRotatingSlotCount()) {
        match.feedback().actionBarWarn("<yellow>该刷新格尚未解锁。");
        return false;
      }
      if (shop.isRotatingSlotSold(rotatingSlotIndex)) {
        match.feedback().actionBarWarn("<yellow>该格本回合已售出。");
        return false;
      }
    }

    int price = offer.priceBones();
    var currency = match.currency(buyerSide);
    if (price > 0 && !currency.trySpendBones(price)) {
      match.feedback().actionBarWarn("<red>骨币不足 ×%d".formatted(price));
      return false;
    }
    String templateId = offer.templateId();
    match.grantCardToHandSilent(buyerSide, templateId);
    match.participant(buyerSide).mainDeck().addLast(templateId);
    if (lane == ShopPurchaseLane.PERMANENT) {
      shop.markPermanentPurchased();
    } else {
      shop.markRotatingSold(rotatingSlotIndex);
    }
    match.completeShopPurchase(buyerSide);
    return true;
  }
}
