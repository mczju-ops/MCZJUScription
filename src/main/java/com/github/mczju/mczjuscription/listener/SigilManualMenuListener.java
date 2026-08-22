package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.menu.SigilManualMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

/** 印记说明书：丢弃键清除搜索筛选。 */
public final class SigilManualMenuListener implements Listener {

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof SigilManualMenu menu)) {
      return;
    }
    if (!isDropAction(event.getAction())) {
      return;
    }
    int raw = event.getRawSlot();
    if (raw != SigilManualMenu.SEARCH_SLOT || !menu.hasActiveSearch()) {
      return;
    }
    event.setCancelled(true);
    menu.clearSearch();
  }

  private static boolean isDropAction(InventoryAction action) {
    return action == InventoryAction.DROP_ALL_SLOT
        || action == InventoryAction.DROP_ONE_SLOT
        || action == InventoryAction.DROP_ALL_CURSOR
        || action == InventoryAction.DROP_ONE_CURSOR;
  }
}
