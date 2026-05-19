package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.player.InscriptionLeaveSupport;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/** 单人进行中对 {@code /mgc leave} 先确认再退出。 */
public final class LeaveConfirmListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLeaveCommand(PlayerCommandPreprocessEvent event) {
        if (!InscriptionLeaveSupport.isLeaveCommand(event.getMessage())) {
            return;
        }
        if (!InscriptionLeaveSupport.isSoloMatchInProgress(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        InscriptionLeaveSupport.promptLeaveConfirm(event.getPlayer());
    }
}
