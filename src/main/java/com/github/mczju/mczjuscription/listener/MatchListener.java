package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.arena.ArenaClockPlacement;
import com.github.mczju.mczjuscription.arena.ArenaManager;
import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.combat.BeamTargeting;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.turn.TurnPhase;
import com.github.mczju.mczjuscription.item.InscriptionCardItem;
import com.github.mczju.mczjuscription.item.InscriptionItemUtil;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.menu.SigilManualMenu;
import com.github.mczju.mczjuscription.ui.MatchHotbar;
import com.github.mczju.mczjuscription.roguelike.WanderingTraderService;
import com.github.mczju.mczjuscription.shop.ShopVillagerService;
import com.github.mczju.mczjuscription.ui.ResourceHotbar;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class MatchListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(player);
        if (match == null) return;

        MatchSide side = match.sideFor(player);
        if (side == null) return;

        if (tryRingBellAtLocation(event, match, player)) {
            return;
        }

        ItemStack item = heldItem(event, player);
        if (item == null) return;

        if (InscriptionItems.drawDeck().isThis(item) || isTool(item, InscriptionItems.drawDeck())) {
            event.setCancelled(true);
            match.drawFromMainDeck(side);
            return;
        }
        if (InscriptionItems.sigilManual().isThis(item) || isTool(item, InscriptionItems.sigilManual())) {
            event.setCancelled(true);
            new SigilManualMenu(player, 0).open();
            return;
        }

        MGCItem mgcItem = MCZJUGameCore.getItemManager().get(item);
        if (mgcItem instanceof InscriptionCardItem) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        // PlayerInteractEntityEvent 仅在右键实体时触发
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(player);
        if (match == null) return;

        MatchSide side = match.sideFor(player);
        if (side == null) return;

        if (ShopVillagerService.isShopVillager(event.getRightClicked(), match)) {
            event.setCancelled(true);
            ShopVillagerService.tryOpenShop(player, match);
            return;
        }

        if (WanderingTraderService.isTraderEntity(event.getRightClicked(), match)) {
            event.setCancelled(true);
            WanderingTraderService.tryOpenMenu(player, match);
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return;

        if (InscriptionItems.sacrificeSword().isThis(hand) || isTool(hand, InscriptionItems.sacrificeSword())) {
            event.setCancelled(true);
            BoardCreature creature = match.findCreatureByEntity(event.getRightClicked().getUniqueId());
            if (creature != null
                && BeamTargeting.handleSacrificeClick(match, side, creature)) {
                return;
            }
            if (creature != null && creature.owner() == side) {
                int value = creature.definition().sacrificeValue();
                if (value >= 2) {
                    new AlertMenu(player, () -> match.sacrifice(creature, side)).open();
                } else {
                    match.sacrifice(creature, side);
                }
            }
            return;
        }

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(player);
        if (match == null) return;

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            int held = player.getInventory().getHeldItemSlot();
            if (MatchHotbar.isLockedSlot(held, match.deckMode())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            int hotbar = event.getHotbarButton();
            if (hotbar >= 0 && MatchHotbar.isLockedSlot(hotbar, match.deckMode())) {
                event.setCancelled(true);
                return;
            }
        }

        if (event.getClickedInventory() == player.getInventory()) {
            int slot = event.getSlot();
            if (MatchHotbar.isLockedSlot(slot, match.deckMode())) {
                event.setCancelled(true);
                return;
            }
        }

        if (ResourceHotbar.isResourceItem(event.getCurrentItem()) || ResourceHotbar.isResourceItem(event.getCursor())) {
            event.setCancelled(true);
        }
        if (MatchHotbar.isInscriptionTool(event.getCurrentItem()) || MatchHotbar.isInscriptionTool(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(player);
        if (match == null) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (int raw : event.getRawSlots()) {
            if (raw < topSize) continue;
            int rel = raw - topSize;
            if (rel < 9 && MatchHotbar.isLockedSlot(rel, match.deckMode())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(event.getPlayer());
        if (match == null) return;

        ItemStack stack = event.getItemDrop().getItemStack();
        if (ResourceHotbar.isResourceItem(stack) || MatchHotbar.isInscriptionTool(stack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        ArenaManager.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        InscriptionItems.stripPlayerInventory(event.getPlayer());
    }

    /** 已不在对局中但背包仍残留骨币/腐肉时，允许丢弃清理。 */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDropCleanup(PlayerDropItemEvent event) {
        if (InscriptionGameAccess.resolveMatch(event.getPlayer()) != null) {
            return;
        }
        if (!ResourceHotbar.isResourceItem(event.getItemDrop().getItemStack())) {
            return;
        }
        event.getItemDrop().remove();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakProtectedBlocks(BlockBreakEvent event) {
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(event.getPlayer());
        if (match == null || match.arena() == null) return;
        BattleArena arena = match.arena();
        MatchSide side = match.sideFor(event.getPlayer());
        if (side != null && ArenaClockPlacement.isClockBlock(arena, event.getBlock(), side)) {
            event.setCancelled(true);
        }
    }

    private static boolean tryRingBellAtLocation(PlayerInteractEvent event, InscriptionMatch match, Player player) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;
        MatchSide side = match.sideFor(player);
        if (side == null) return false;
        BattleArena arena = match.arena();
        if (arena == null || !arena.hasClock(side)) return false;

        Block clicked = event.getClickedBlock();
        if (clicked != null && ArenaClockPlacement.isClockBlock(arena, clicked, side)) {
            event.setCancelled(true);
            tryRingBell(match, player);
            return true;
        }

        Location point = event.getInteractionPoint();
        if (point == null && clicked != null) {
            point = clicked.getLocation().add(0.5, 0.5, 0.5);
        }
        Location clockCenter = arena.clockLocation(side);
        if (point != null && clockCenter != null && ArenaClockPlacement.isClockInteract(point, clockCenter)) {
            event.setCancelled(true);
            tryRingBell(match, player);
            return true;
        }
        return false;
    }

    private static void tryRingBell(InscriptionMatch match, Player player) {
        if (match.isCombatAnimating()) {
            match.feedback().actionBarWarn("<yellow>战斗进行中");
            return;
        }
        match.turn().ringBell();
    }

    private static ItemStack heldItem(PlayerInteractEvent event, Player player) {
        ItemStack item = event.getItem();
        if (item == null || item.getType().isAir()) {
            item = player.getInventory().getItemInMainHand();
        }
        if (item == null || item.getType().isAir()) return null;
        return item;
    }

    private static boolean isTool(ItemStack stack, com.github.mczju.mczjuscription.item.InscriptionToolItem tool) {
        return InscriptionItemUtil.isTool(stack, tool);
    }

}
