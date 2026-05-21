package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.shop.ShopConfig;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import com.github.mczju.mczjuscription.shop.ShopOffer;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

public final class ShopAdminPermanentMenu extends Menu {

  public static final int[] SLOTS = {19, 20, 21, 22};
  public static final int HAND_PLACE_SLOT = 40;

  public ShopAdminPermanentMenu(Player player) {
    super(player);
  }

  @Override
  protected void setup() {
    inventory.clear();
    ShopConfig cfg = ShopConfigStorage.get();

    for (int i = 0; i < SLOTS.length; i++) {
      int ui = SLOTS[i];
      int index = i;
      ShopOffer offer = cfg.permanent(index);
      if (offer.isEmpty()) {
        setSlot(
            ui,
            ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .customName("<gray>常驻 #" + (index + 1) + " · 空")
                .lore(
                    List.of(
                        "<yellow>手持卡牌点击对应格放入",
                        "<dark_gray>有货后：左键价+1 右键价-1",
                        "<red>丢弃键删除"))
                .build(),
            (p, e) -> {});
      } else {
        CardTemplate def = CardCatalog.require(offer.templateId());
        setSlot(
            ui,
            ItemBuilder.of(def.spawnEggMaterial())
                .customName("<aqua>常驻 #" + (index + 1) + " · " + def.displayName())
                .lore(
                    List.of(
                        "<gray>ID: " + offer.templateId(),
                        "<gold>骨币: " + offer.priceBones(),
                        "<gray>左键价+1 右键价-1",
                        "<red>丢弃键删除"))
                .build(),
            (p, e) -> {
              if (e.isShiftClick()) {
                return;
              }
              if (e.isLeftClick()) {
                adjustPermanentPrice(cfg, index, offer, true);
              } else if (e.isRightClick()) {
                adjustPermanentPrice(cfg, index, offer, false);
              }
            });
      }
    }

    setSlot(
        HAND_PLACE_SLOT,
        ItemBuilder.of(Material.CHEST)
            .customName("<yellow>放入卡牌")
            .lore(List.of("<gray>手持邪恶冥刻卡牌", "<gray>点击本格或对应常驻格"))
            .build(),
        (p, e) -> {});

    setSlot(
        49,
        ItemBuilder.of(Material.ARROW).customName("<gray>返回").build(),
        (p, e) -> new ShopAdminMenu(p.player()).open());
  }

  public void placeFromHand(int permanentIndex, String templateId) {
    ShopConfig cfg = ShopConfigStorage.get();
    ShopOffer current = cfg.permanent(permanentIndex);
    int price = current.isEmpty() ? 2 : current.priceBones();
    cfg.setPermanent(permanentIndex, ShopOffer.of(templateId, price));
    refresh();
  }

  public void clearPermanent(int permanentIndex) {
    ShopConfigStorage.get().setPermanent(permanentIndex, ShopOffer.empty());
    refresh();
  }

  private void adjustPermanentPrice(
      ShopConfig cfg, int index, ShopOffer offer, boolean increase) {
    int price = offer.priceBones() + (increase ? 1 : -1);
    cfg.setPermanent(index, ShopOffer.of(offer.templateId(), Math.max(0, price)));
    refresh();
  }

  public void refresh() {
    setup();
  }

  @Override
  protected String getTitle() {
    return "常驻货架";
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.admin";
  }
}
