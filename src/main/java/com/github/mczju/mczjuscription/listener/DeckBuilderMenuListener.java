package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.menu.DeckViewMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class DeckBuilderMenuListener implements Listener {

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onClick(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof DeckViewMenu menu)) {
      return;
    }
    if (!isDropAction(event.getAction())) {
      return;
    }
    int raw = event.getRawSlot();
    if (raw < 0 || raw >= event.getView().getTopInventory().getSize()) {
      return;
    }
    String cardId = menu.cardIdAtSlot(raw);
    if (cardId == null) {
      return;
    }
    event.setCancelled(true);
    menu.removeOne(cardId);
  }

  private static boolean isDropAction(InventoryAction action) {
    return action == InventoryAction.DROP_ALL_SLOT
        || action == InventoryAction.DROP_ONE_SLOT
        || action == InventoryAction.DROP_ALL_CURSOR
        || action == InventoryAction.DROP_ONE_CURSOR;
  }
}
