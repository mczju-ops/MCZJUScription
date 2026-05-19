package com.github.mczju.mczjuscription.listener;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.arena.ArenaSlotPlacement;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.turn.TurnPhase;
import com.github.mczju.mczjuscription.item.InscriptionCardItem;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.OptionalInt;
import java.util.UUID;

/** 出牌：将卡牌（刷怪蛋）丢到己方槽位方块上召唤。 */
public final class CardDropPlacementListener implements Listener {

    private static final int TRACK_MAX_TICKS = 120;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        InscriptionMatch match = InscriptionGameAccess.resolveMatch(player);
        if (match == null) return;

        CardId cardId = resolveCardId(event.getItemDrop().getItemStack());
        if (cardId == null) return;

        MatchSide side = match.sideFor(player);
        if (side == null) {
            event.setCancelled(true);
            return;
        }

        if (match.isCombatAnimating()) {
            event.setCancelled(true);
            match.feedback().actionBarWarn("<yellow>战斗进行中");
            return;
        }
        if (match.turn().phase() != TurnPhase.PLAY || side != match.actingSide()) {
            event.setCancelled(true);
            return;
        }
        if (!match.participant(side).hand().contains(cardId) || match.arena() == null) {
            event.setCancelled(true);
            return;
        }

        Item drop = event.getItemDrop();
        drop.getPersistentDataContainer().set(
                InscriptionKeys.DROP_CARD,
                PersistentDataType.STRING,
                cardId.name()
        );
        drop.getPersistentDataContainer().set(
                InscriptionKeys.DROP_PLAYER,
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );
        trackDrop(match, side, cardId, drop);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Item item = event.getItem();
        if (!item.getPersistentDataContainer().has(InscriptionKeys.DROP_CARD, PersistentDataType.STRING)) {
            return;
        }
        String ownerId = item.getPersistentDataContainer().get(InscriptionKeys.DROP_PLAYER, PersistentDataType.STRING);
        if (ownerId != null && ownerId.equals(player.getUniqueId().toString())) {
            return;
        }
        event.setCancelled(true);
    }

    private void trackDrop(InscriptionMatch match, MatchSide side, CardId cardId, Item drop) {
        new BukkitRunnable() {
            int ticks;

            @Override
            public void run() {
                if (!drop.isValid() || match.isMatchOver()) {
                    cancel();
                    return;
                }
                if (++ticks > TRACK_MAX_TICKS) {
                    cancel();
                    return;
                }
                if (!drop.isOnGround() && drop.getVelocity().lengthSquared() > 0.003) {
                    return;
                }
                cancel();
                tryPlace(match, side, cardId, drop);
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 2L, 1L);
    }

    private void tryPlace(InscriptionMatch match, MatchSide side, CardId cardId, Item drop) {
        if (!drop.isValid()) return;

        Player player = resolveDropPlayer(drop.getPersistentDataContainer().get(
                InscriptionKeys.DROP_PLAYER, PersistentDataType.STRING));
        if (player == null || match.sideFor(player) != side) {
            drop.remove();
            return;
        }

        SlotOwner row = BoardSides.toSlotOwner(side);
        OptionalInt slotIndex = ArenaSlotPlacement.findSlotIndex(match.arena(), drop.getLocation(), row);
        if (slotIndex.isEmpty()) {
            return;
        }

        if (match.playCardToSlot(cardId, slotIndex.getAsInt(), side, drop)) {
            drop.getPersistentDataContainer().remove(InscriptionKeys.DROP_CARD);
            drop.getPersistentDataContainer().remove(InscriptionKeys.DROP_PLAYER);
        }
    }

    private static Player resolveDropPlayer(String uuidRaw) {
        if (uuidRaw == null) return null;
        try {
            return org.bukkit.Bukkit.getPlayer(UUID.fromString(uuidRaw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static CardId resolveCardId(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return null;
        MGCItem mgc = MCZJUGameCore.getItemManager().get(stack);
        if (mgc instanceof InscriptionCardItem cardItem) {
            return cardItem.cardId();
        }
        return null;
    }
}
