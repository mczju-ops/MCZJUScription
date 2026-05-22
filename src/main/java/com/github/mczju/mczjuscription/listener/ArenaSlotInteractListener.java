package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.arena.ArenaPedalTarget;
import com.github.mczju.mczjuscription.arena.ArenaSlotInteractService;
import com.github.mczju.mczjuscription.arena.ArenaSlotRay;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/** 隔空左键射线命中场地踏板（出牌 / 献祭 / 射线 / 商店 / 敲钟）。 */
public final class ArenaSlotInteractListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(player);
        if (match == null || match.arena() == null) {
            return;
        }

        ArenaPedalTarget target = ArenaSlotRay.tracePedal(player, match.arena());
        if (target == null) {
            return;
        }

        if (ArenaSlotInteractService.tryInteract(player, match, target)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ArenaSlotInteractService.clearPlayer(event.getPlayer().getUniqueId());
    }
}
