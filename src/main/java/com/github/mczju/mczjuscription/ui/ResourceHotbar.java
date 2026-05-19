package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.game.match.Currency;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class ResourceHotbar {

    public static final int BONE_SLOT = MatchHotbar.BONE_SLOT;
    public static final int FLESH_SLOT = MatchHotbar.FLESH_SLOT;

    private ResourceHotbar() {}

    public static void sync(Player player, Currency currency) {
        player.getInventory().setItem(BONE_SLOT, resourceStack(
                Material.BONE,
                "bone",
                "<gold>骨币",
                currency.getBones()
        ));
        player.getInventory().setItem(FLESH_SLOT, resourceStack(
                Material.ROTTEN_FLESH,
                "flesh",
                "<red>腐肉",
                currency.getBlood()
        ));
    }

    public static boolean isResourceItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(InscriptionKeys.RESOURCE_ITEM, PersistentDataType.STRING);
    }

    /** 强制清空骨币/腐肉槽及背包内所有资源显示物。 */
    public static void clear(Player player) {
        var inv = player.getInventory();
        inv.setItem(BONE_SLOT, null);
        inv.setItem(FLESH_SLOT, null);
        for (int i = 0; i < inv.getSize(); i++) {
            if (isResourceItem(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }
        if (isResourceItem(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
        }
    }

    private static ItemStack resourceStack(Material material, String id, String name, int amount) {
        int displayAmount = Math.max(1, Math.min(64, amount));
        ItemStack stack = ItemBuilder.of(material)
                .customName(name + " <gray>×" + amount)
                .amount(displayAmount)
                .build();
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(
                InscriptionKeys.RESOURCE_ITEM,
                PersistentDataType.STRING,
                id
        ));
        return stack;
    }
}
