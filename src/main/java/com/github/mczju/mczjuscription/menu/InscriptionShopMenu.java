package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.shop.ParticipantShopState;
import com.github.mczju.mczjuscription.shop.ShopConfig;
import com.github.mczju.mczjuscription.shop.ShopLayout;
import com.github.mczju.mczjuscription.shop.ShopOffer;
import com.github.mczju.mczjuscription.shop.ShopPresenter;
import com.github.mczju.mczjuscription.shop.ShopPurchaseLane;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

public final class InscriptionShopMenu extends Menu {

  private final InscriptionMatch match;
  private final MatchSide buyerSide;
  private final ShopPresenter presenter;

  public InscriptionShopMenu(
      Player player, InscriptionMatch match, MatchSide buyerSide, ShopPresenter presenter) {
    super(player, match, buyerSide, presenter);
    this.match = match;
    this.buyerSide = buyerSide;
    this.presenter = presenter;
  }

  @Override
  protected void setup() {
    inventory.clear();
    int bones = match.currency(buyerSide).getBones();
    int blood = match.currency(buyerSide).getBlood();
    int fish = match.currency(buyerSide).getFish();
    ShopConfig config = match.shopConfig();
    ParticipantShopState state = match.shopState(buyerSide);
    boolean permanentBlocked = !state.canBuyPermanentThisTurn();
    int permanentRemaining = Math.max(0, ParticipantShopState.MAX_PERMANENT_PER_TURN - state.permanentPurchasesThisTurn());
    boolean extraUnlocked = state.isExtraRotatingSlotUnlocked();
    int unlockCost = config.extraSlotUnlockBlood();

    setSlot(
        ShopLayout.INFO,
        ItemBuilder.of(Material.GOLD_INGOT)
            .customName("<gold>卡牌商店")
            .lore(
                List.of(
                    "<gray>骨币: <gold>" + bones + "  <gray>腐肉: <red>" + blood + "  <gray>鱼干: <aqua>" + fish,
                    "<aqua>常驻×4: <white>每回合总共可买 "
                        + ParticipantShopState.MAX_PERMANENT_PER_TURN
                        + " 张 <gray>(剩余 " + permanentRemaining + ")",
                    "<yellow>刷新×3: <white>每格每回合只能买 1 次",
                    extraUnlocked
                        ? "<green>第 4 刷新格已解锁"
                        : "<gray>第 4 格: 腐肉×" + unlockCost + " 解锁（本局）"))
            .build(),
        (p, e) -> {});

    for (int i = 0; i < ShopLayout.PERMANENT.length; i++) {
      int slot = ShopLayout.PERMANENT[i];
      ShopOffer offer = config.permanent(i);
      placeOffer(
          slot, offer, bones, ShopPurchaseLane.PERMANENT, permanentBlocked, -1, permanentRemaining);
    }

    for (int i = 0; i < ShopLayout.ROTATING.length; i++) {
      int layoutSlot = ShopLayout.ROTATING[i];
      if (i == ShopConfig.MAX_ROTATING_SLOTS - 1 && !extraUnlocked) {
        placeLockedRotatingSlot(layoutSlot, unlockCost, blood);
        continue;
      }
      if (state.isRotatingSlotSold(i)) {
        placeSoldRotatingSlot(layoutSlot);
        continue;
      }
      ShopOffer offer = state.rotatingOffer(i);
      placeOffer(layoutSlot, offer, bones, ShopPurchaseLane.ROTATING, false, i, -1);
    }

    setSlot(
        ShopLayout.CLOSE,
        ItemBuilder.of(Material.BARRIER)
            .customName("<red>关闭")
            .lore(List.of("<gray>关闭商店", "<gray>可继续购卡或出牌"))
            .build(),
        (p, e) -> p.player().closeInventory());
  }

  private void placeLockedRotatingSlot(int slot, int unlockCost, int blood) {
    boolean affordable = blood >= unlockCost;
    List<String> lore = new ArrayList<>();
    lore.add("<gray>本局永久增加 1 个刷新格");
    lore.add("<gray>花费: <red>腐肉 ×" + unlockCost);
    lore.add(affordable ? "<green>点击解锁" : "<red>腐肉不足");
    setSlot(
        slot,
        ItemBuilder.of(Material.IRON_BARS)
            .customName("<red>锁定 · 第 4 刷新格")
            .lore(lore)
            .build(),
        (p, e) -> {
          if (match.tryUnlockExtraRotatingSlot(buyerSide)) {
            setup();
          }
        });
  }

  private void placeSoldRotatingSlot(int slot) {
    setSlot(
        slot,
        ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
            .customName("<dark_gray>已售出")
            .lore(List.of("<gray>本回合刷新", "<dark_gray>下回合会换新货"))
            .build(),
        (p, e) ->
            match.feedback().actionBarWarn("<yellow>该刷新格本回合已售出。"));
  }

  private void placeOffer(
      int slot,
      ShopOffer offer,
      int bones,
      ShopPurchaseLane lane,
      boolean permanentBlocked,
      int rotatingSlotIndex,
      int permanentRemaining) {
    String tag = lane == ShopPurchaseLane.PERMANENT ? "常驻" : "本回合刷新";
    if (offer.isEmpty()) {
      List<String> emptyLore = new ArrayList<>();
      emptyLore.add("<gray>" + tag);
      if (lane == ShopPurchaseLane.PERMANENT && permanentRemaining >= 0) {
        emptyLore.add(
            "<gray>常驻造物购买次数还剩 <white>" + permanentRemaining + "<gray> 次");
      }
      setSlot(
          slot,
          ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
              .customName("<dark_gray>空位")
              .lore(emptyLore)
              .build(),
          (p, e) -> {});
      return;
    }
    CardTemplate def = CardCatalog.require(offer.templateId());
    int price = offer.priceBones();
    boolean affordable = price == 0 || bones >= price;
    boolean blocked = lane == ShopPurchaseLane.PERMANENT && permanentBlocked;
    List<String> lore = new ArrayList<>();
    lore.add("<gray>" + tag);
    lore.add("<gray>力/命 <white>" + def.power() + "/" + def.health());
    lore.add("<dark_purple>印记: <light_purple>" + def.sigilsDisplay());
    if (lane == ShopPurchaseLane.PERMANENT) {
      if (permanentRemaining >= 0) {
        lore.add("<gray>常驻造物购买次数还剩 <white>" + permanentRemaining + "<gray> 次");
      }
    } else {
      lore.add("<dark_gray>本格本回合仅可购 1 次");
    }
    lore.add("<gray>价格: <gold>" + (price == 0 ? "免费" : price + " 骨币"));
    if (!blocked) {
      if (affordable) {
        lore.add("<green>点击购买");
      } else {
        lore.add("<red>骨币不足");
      }
    }
    setSlot(
        slot,
        ItemBuilder.of(def.spawnEggMaterial())
            .customName("<gold>" + def.displayName())
            .lore(lore)
            .build(),
        (p, e) -> {
          if (blocked) {
            match.feedback().actionBarWarn("<yellow>本回合常驻货架购卡次数已用完。");
            return;
          }
          if (!affordable) {
            match.feedback().actionBarWarn("<red>骨币不足 ×%d".formatted(price));
            return;
          }
          if (presenter.purchase(match, buyerSide, offer, lane, rotatingSlotIndex)) {
            setup();
          }
        });
  }

  @Override
  protected String getTitle() {
    InscriptionMatch m = match;
    MatchSide side = buyerSide;
    if (m == null && args.length >= 1 && args[0] instanceof InscriptionMatch found) {
      m = found;
    }
    if (side == null && args.length >= 2 && args[1] instanceof MatchSide found) {
      side = found;
    }
    if (m == null || side == null) {
      return "卡牌商店";
    }
    return "卡牌商店 · 骨币 " + m.currency(side).getBones();
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 5;
  }

  @Override
  protected String getPermission() {
    return "inscription.play";
  }
}
