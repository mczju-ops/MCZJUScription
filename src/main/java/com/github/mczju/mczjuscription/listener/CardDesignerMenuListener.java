package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.menu.CardDesignerMenu;
import com.github.mczju.mczjuscription.util.SpawnEggEntityTypes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** 设计器：将背包中的怪物蛋放入模型槽以选定实体类型（不消耗物品）。 */
public final class CardDesignerMenuListener implements Listener {

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getView().getTopInventory().getHolder() instanceof CardDesignerMenu menu)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();
    Inventory top = event.getView().getTopInventory();
    int topSize = top.getSize();

    if (event.getRawSlot() == CardDesignerMenu.MODEL_SLOT
            && event.getClickedInventory() == top) {
      ItemStack onCursor = event.getCursor();
      if (applyEgg(menu, onCursor)) {
        event.setCancelled(true);
        menu.reloadEditor();
        player.updateInventory();
      }
      return;
    }

    if (event.getRawSlot() >= topSize) {
      ItemStack clicked = event.getCurrentItem();
      if (applyEgg(menu, clicked)) {
        event.setCancelled(true);
        menu.reloadEditor();
        player.updateInventory();
      }
    } else if (event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
            && event.getHotbarButton() >= 0) {
      ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
      if (event.getRawSlot() == CardDesignerMenu.MODEL_SLOT && applyEgg(menu, hotbar)) {
        event.setCancelled(true);
        menu.reloadEditor();
        player.updateInventory();
      }
    }
  }

  private static boolean applyEgg(CardDesignerMenu menu, ItemStack stack) {
    return SpawnEggEntityTypes.fromSpawnEgg(stack)
            .map(
                    type -> {
                      menu.session().setEntityType(type);
                      return true;
                    })
            .orElse(false);
  }
}
