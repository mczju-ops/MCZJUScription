package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.item.InscriptionCardItem;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ShopCardItems {

  private ShopCardItems() {}

  public static @Nullable String templateIdFromStack(ItemStack stack) {
    if (stack == null || stack.getType().isAir()) {
      return null;
    }
    MGCItem mgc = MCZJUGameCore.getItemManager().get(stack);
    if (mgc instanceof InscriptionCardItem card) {
      return card.templateId();
    }
    return null;
  }

  public static @Nullable String templateIdFromHand(Player player) {
    return templateIdFromStack(player.getInventory().getItemInMainHand());
  }

  public static boolean isKnownTemplate(String templateId) {
    return templateId != null && CardCatalog.exists(templateId);
  }
}
