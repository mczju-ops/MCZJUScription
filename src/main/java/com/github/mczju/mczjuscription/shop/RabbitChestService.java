package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

/** @deprecated 兔子堆已取消，保留空壳避免旧引用编译失败。 */
@Deprecated
public final class RabbitChestService {

    private RabbitChestService() {}

    public static void refreshLabels(InscriptionMatch match) {}

    public static boolean tryClaimFromEntity(
            PlayerInteractEntityEvent event, InscriptionMatch match, MatchSide side) {
        return false;
    }

    public static void onInventoryClick(
            InventoryClickEvent event, InscriptionMatch match, MatchSide side) {}

    public static void onInventoryDrag(
            InventoryDragEvent event, InscriptionMatch match, MatchSide side) {}

    public static void onChestOpen(InventoryOpenEvent event, InscriptionMatch match, MatchSide side) {}

    public static void onChestClose(InventoryCloseEvent event, InscriptionMatch match, MatchSide side) {}

    public static void onClaimed(InscriptionMatch match, MatchSide side) {}
}
