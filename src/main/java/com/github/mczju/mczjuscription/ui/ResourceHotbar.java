package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.util.InscriptionKeys;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** 旧版快捷栏代币显示物清理（代币现由场地 UI 展示）。 */
public final class ResourceHotbar {

    private ResourceHotbar() {}

    public static boolean isResourceItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(InscriptionKeys.RESOURCE_ITEM, PersistentDataType.STRING);
    }

    /** 清空背包内残留的资源显示物。 */
    public static void clear(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isResourceItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        if (isResourceItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }
}
