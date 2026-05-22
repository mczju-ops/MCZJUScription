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
    /** 快捷栏最后一格：印记说明书。 */
    public static final int SIGIL_MANUAL_SLOT = 8;

    private MatchHotbar() {}

    public static boolean isReservedToolSlot(int slot, DeckMode deckMode) {
        if (slot == SACRIFICE_SLOT || slot == SIGIL_MANUAL_SLOT) {
            return true;
        }
        if (deckMode == DeckMode.SHOP) {
            return false;
        }
        return slot == MAIN_DECK_SLOT || slot == RABBIT_PILE_SLOT;
    }

    public static boolean isReservedToolSlot(int slot) {
        return isReservedToolSlot(slot, DeckMode.FREE_BUILD);
    }

    public static boolean isLockedSlot(int slot, DeckMode deckMode) {
        return isReservedToolSlot(slot, deckMode);
    }

    public static boolean isLockedSlot(int slot) {
        return isLockedSlot(slot, DeckMode.FREE_BUILD);
    }

    /** 开局与 HUD 同步时，把工具放回指定格子。 */
    public static void placeTools(Player player, DeckMode deckMode) {
        PlayerInventory inv = player.getInventory();
        ResourceHotbar.clear(player);
        clearLegacySigilManual(inv, 5);
        inv.setItem(SACRIFICE_SLOT, InscriptionItems.sacrificeSword().getItem());
        if (deckMode == DeckMode.SHOP) {
            clearLegacyToolSlot(inv, MAIN_DECK_SLOT);
            clearLegacyToolSlot(inv, RABBIT_PILE_SLOT);
        } else {
            inv.setItem(MAIN_DECK_SLOT, InscriptionItems.drawDeck().getItem());
            inv.setItem(RABBIT_PILE_SLOT, InscriptionItems.rabbitPile().getItem());
        }
        inv.setItem(SIGIL_MANUAL_SLOT, InscriptionItems.sigilManual().getItem());
    }

    /** 仅移除旧版快捷栏工具，避免清空已购入的手牌。 */
    private static void clearLegacyToolSlot(PlayerInventory inv, int slot) {
        ItemStack stack = inv.getItem(slot);
        if (isInscriptionTool(stack)) {
            inv.setItem(slot, null);
        }
    }

    private static void clearLegacySigilManual(PlayerInventory inv, int slot) {
        ItemStack stack = inv.getItem(slot);
        if (stack != null && InscriptionItems.sigilManual().isThis(stack)) {
            inv.setItem(slot, null);
        }
    }

    /** 手牌优先落点（商店模式 1–7 均可放手牌）。 */
    public static int[] handCardSlots(DeckMode deckMode) {
        if (deckMode == DeckMode.SHOP) {
            return new int[] {3, 4, 5, 6, 7, 1, 2};
        }
        return new int[] {3, 4, 5, 6, 7};
    }

    public static boolean isInscriptionTool(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        return InscriptionItems.sacrificeSword().isThis(stack)
                || InscriptionItems.drawDeck().isThis(stack)
                || InscriptionItems.shopDeck().isThis(stack)
                || InscriptionItems.rabbitPile().isThis(stack)
                || InscriptionItems.sigilManual().isThis(stack);
    }
}
