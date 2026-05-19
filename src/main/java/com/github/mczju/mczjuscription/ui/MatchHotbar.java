package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** 对局内快捷栏固定布局。 */
public final class MatchHotbar {

    public static final int SACRIFICE_SLOT = 0;
    public static final int MAIN_DECK_SLOT = 1;
    public static final int RABBIT_PILE_SLOT = 2;
    public static final int BONE_SLOT = 7;
    public static final int FLESH_SLOT = 8;

    private MatchHotbar() {}

    public static boolean isReservedToolSlot(int slot) {
        return slot == SACRIFICE_SLOT || slot == MAIN_DECK_SLOT || slot == RABBIT_PILE_SLOT;
    }

    public static boolean isResourceSlot(int slot) {
        return slot == BONE_SLOT || slot == FLESH_SLOT;
    }

    public static boolean isLockedSlot(int slot) {
        return isReservedToolSlot(slot) || isResourceSlot(slot);
    }

    /** 开局与 HUD 同步时，把工具放回指定格子。 */
    public static void placeTools(Player player, DeckMode deckMode) {
        PlayerInventory inv = player.getInventory();
        inv.setItem(SACRIFICE_SLOT, InscriptionItems.sacrificeSword().getItem());
        inv.setItem(MAIN_DECK_SLOT, mainDeckItem(deckMode));
        inv.setItem(RABBIT_PILE_SLOT, InscriptionItems.rabbitPile().getItem());
    }

    private static ItemStack mainDeckItem(DeckMode deckMode) {
        return deckMode == DeckMode.SHOP
                ? InscriptionItems.shopDeck().getItem()
                : InscriptionItems.drawDeck().getItem();
    }

    public static boolean isInscriptionTool(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        return InscriptionItems.sacrificeSword().isThis(stack)
                || InscriptionItems.drawDeck().isThis(stack)
                || InscriptionItems.shopDeck().isThis(stack)
                || InscriptionItems.rabbitPile().isThis(stack);
    }
}
