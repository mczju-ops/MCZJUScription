package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.menu.ShopAdminPermanentMenu;
import com.github.mczju.mczjuscription.menu.ShopAdminPoolMenu;
import com.github.mczju.mczjuscription.shop.ShopCardItems;
import com.github.mczju.mczjuscription.shop.ShopPoolEntry;
import com.github.mczju.mczjuscription.ui.DialogTextInput;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public final class ShopAdminListener implements Listener {

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }
    Inventory top = event.getView().getTopInventory();
    var holder = top.getHolder();

    if (holder instanceof ShopAdminPermanentMenu menu) {
      if (tryHandleDrop(event, menu)) {
        return;
      }
      handlePermanentPlace(event, player, menu);
    } else if (holder instanceof ShopAdminPoolMenu poolMenu) {
      if (tryHandlePoolShiftLeftWeight(event, player, poolMenu)) {
        return;
      }
      if (tryHandlePoolDrop(event, poolMenu)) {
        return;
      }
      handlePoolAdd(event, player, poolMenu);
    }
  }

  private static boolean tryHandleDrop(InventoryClickEvent event, ShopAdminPermanentMenu menu) {
    if (!isDropAction(event.getAction())) {
      return false;
    }
    int raw = event.getRawSlot();
    if (raw < 0 || raw >= event.getView().getTopInventory().getSize()) {
      return false;
    }
    for (int i = 0; i < ShopAdminPermanentMenu.SLOTS.length; i++) {
      if (raw == ShopAdminPermanentMenu.SLOTS[i]) {
        event.setCancelled(true);
        menu.clearPermanent(i);
        return true;
      }
    }
    return false;
  }

  private static boolean tryHandlePoolShiftLeftWeight(
      InventoryClickEvent event, Player player, ShopAdminPoolMenu menu) {
    if (!event.isShiftClick() || !event.isLeftClick()) {
      return false;
    }
    int raw = event.getRawSlot();
    if (raw < 0 || raw >= ShopAdminPoolMenu.ADD_SLOT || raw >= menu.pool().size()) {
      return false;
    }
    event.setCancelled(true);
    ShopPoolEntry entry = menu.pool().get(raw);
    menu.setSelectedIndex(raw);
    DialogTextInput.openShopWeight(player, raw, entry.weight());
    return true;
  }

  private static boolean tryHandlePoolDrop(InventoryClickEvent event, ShopAdminPoolMenu menu) {
    if (!isDropAction(event.getAction())) {
      return false;
    }
    int raw = event.getRawSlot();
    if (raw < 0 || raw >= ShopAdminPoolMenu.ADD_SLOT || raw >= menu.pool().size()) {
      return false;
    }
    event.setCancelled(true);
    menu.removeAt(raw);
    return true;
  }

  private static boolean isDropAction(InventoryAction action) {
    return action == InventoryAction.DROP_ALL_SLOT
        || action == InventoryAction.DROP_ONE_SLOT
        || action == InventoryAction.DROP_ALL_CURSOR
        || action == InventoryAction.DROP_ONE_CURSOR;
  }

  private static void handlePermanentPlace(
      InventoryClickEvent event, Player player, ShopAdminPermanentMenu menu) {
    int raw = event.getRawSlot();
    int topSize = event.getView().getTopInventory().getSize();
    if (raw >= topSize) {
      return;
    }
    String templateId = ShopCardItems.templateIdFromStack(event.getCursor());
    if (templateId == null) {
      templateId = ShopCardItems.templateIdFromHand(player);
    }
    if (templateId == null || !ShopCardItems.isKnownTemplate(templateId)) {
      return;
    }
    for (int i = 0; i < ShopAdminPermanentMenu.SLOTS.length; i++) {
      if (raw == ShopAdminPermanentMenu.SLOTS[i]
          || raw == ShopAdminPermanentMenu.HAND_PLACE_SLOT) {
        event.setCancelled(true);
        if (raw == ShopAdminPermanentMenu.HAND_PLACE_SLOT) {
          menu.placeFromHand(0, templateId);
        } else {
          menu.placeFromHand(i, templateId);
        }
        player.updateInventory();
        return;
      }
    }
  }

  private static void handlePoolAdd(
      InventoryClickEvent event, Player player, ShopAdminPoolMenu menu) {
    int raw = event.getRawSlot();
    if (raw != ShopAdminPoolMenu.ADD_SLOT) {
      return;
    }
    String templateId = ShopCardItems.templateIdFromStack(event.getCursor());
    if (templateId == null) {
      templateId = ShopCardItems.templateIdFromHand(player);
    }
    if (templateId == null || !ShopCardItems.isKnownTemplate(templateId)) {
      return;
    }
    event.setCancelled(true);
    menu.addFromHand(templateId);
    player.updateInventory();
  }
}
