package com.github.mczju.mczjuscription.item;

import com.github.mczju.mczjuscription.ui.ResourceHotbar;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.item.ItemManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** 识别背包中的 MGC 注册物品。 */
public final class InscriptionItemUtil {

    private InscriptionItemUtil() {}

    public static boolean isRegistered(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemManager manager = MCZJUGameCore.getItemManager();
        return manager.getItemId(stack) != null;
    }

    public static boolean isTool(ItemStack stack, InscriptionToolItem tool) {
        if (stack == null || stack.getType().isAir() || tool == null) return false;
        return tool.isThis(stack)
                || MCZJUGameCore.getItemManager().is(stack, tool.getId());
    }

    public static void removeTool(Player player, InscriptionToolItem tool) {
        ItemManager manager = MCZJUGameCore.getItemManager();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null && (tool.isThis(stack) || manager.is(stack, tool.getId()))) {
                inv.setItem(i, null);
            }
        }
    }

    /**
     * 移除本插件所有 MGC 物品（卡牌、工具、骨币/腐肉显示物）。
     * 对局结束或新局开始前调用，避免逻辑手牌已清空但背包仍残留上一局的卡。
     */
    public static void clearAllInscriptionItems(Player player) {
        ItemManager manager = MCZJUGameCore.getItemManager();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (shouldRemove(manager, stack)) {
                inv.setItem(i, null);
            }
        }
        if (shouldRemove(manager, inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    private static boolean shouldRemove(ItemManager manager, ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        if (ResourceHotbar.isResourceItem(stack)) return true;
        String id = manager.getItemId(stack);
        return id != null && id.startsWith(InscriptionItems.ID_PREFIX);
    }
}
