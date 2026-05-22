package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.combat.BeamTargeting;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.turn.TurnPhase;
import com.github.mczju.mczjuscription.item.InscriptionCardItem;
import com.github.mczju.mczjuscription.item.InscriptionItemUtil;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.shop.ShopVillagerService;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 隔空左键射线踏板：第一次选中（黄），第二次确认（绿）。 */
public final class ArenaSlotInteractService {

    private static final long FLASH_TICKS = 8L;
    private static final Map<UUID, PendingSelection> PENDING = new ConcurrentHashMap<>();

    private ArenaSlotInteractService() {}

    public static boolean tryInteract(Player player, InscriptionMatch match, ArenaPedalTarget target) {
        if (player == null || match == null || target == null || match.arena() == null) {
            return false;
        }
        MatchSide side = match.sideFor(player);
        if (side == null) {
            return false;
        }

        // 商店 / 敲钟：任意手持（空手、怪物蛋、剑、资源物等）均可
        if (target instanceof ArenaPedalTarget.Ui ui && ui.kind() == ResolvedArenaLayout.UiSlotKind.SHOP) {
            if (ui.side() != side) {
                return false;
            }
            return handleShopSlot(player, match, target);
        }
        if (target instanceof ArenaPedalTarget.BellStrip bell) {
            if (bell.side() != side) {
                return false;
            }
            return handleBellStrip(player, match, target);
        }

        if (target instanceof ArenaPedalTarget.PreviewStrip) {
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (match.beamSession() != null
                && match.beamSession().attackerSide() == side
                && isSacrificeSword(hand)) {
            return BeamTargeting.handlePedalClick(match, side, target);
        }

        if (target instanceof ArenaPedalTarget.Battle) {
            if (isCard(hand)) {
                return handleCardPlay(player, match, side, target, hand);
            }
            if (isSacrificeSword(hand)) {
                return handleSacrificeOrBeam(player, match, side, target);
            }
        }

        return false;
    }

    public static void clearPlayer(UUID playerId) {
        PendingSelection pending = PENDING.remove(playerId);
        if (pending != null && pending.match().arena() != null) {
            pending.match().arena().setPedalHighlight(pending.target(), BattleArena.PedalHighlight.NORMAL);
        }
    }

    private static boolean handleBellStrip(Player player, InscriptionMatch match, ArenaPedalTarget target) {
        if (match.isCombatAnimating()) {
            warn(player, match, "<yellow>战斗进行中");
            return true;
        }
        if (match.turn().phase() != TurnPhase.PLAY) {
            warn(player, match, "<yellow>当前无法敲钟");
            return true;
        }

        PendingSelection pending = PENDING.get(player.getUniqueId());
        if (pending != null && pending.sameTarget(target)) {
            PENDING.remove(player.getUniqueId());
            ArenaSlotSounds.play(player, ArenaSlotSounds.Kind.CONFIRM);
            flash(match, target, BattleArena.PedalHighlight.CONFIRMED, BattleArena.PedalHighlight.NORMAL);
            match.turn().ringBell();
            return true;
        }

        clearPending(player.getUniqueId());
        select(player, match, target);
        match.feedback().actionBarInfo("<yellow>已选中钟，再次左键确认结束回合");
        return true;
    }

    private static boolean handleShopSlot(Player player, InscriptionMatch match, ArenaPedalTarget target) {
        if (match.deckMode() != DeckMode.SHOP) {
            warn(player, match, "<yellow>当前模式无法打开商店");
            return true;
        }
        PendingSelection pending = PENDING.get(player.getUniqueId());
        if (pending != null && pending.sameTarget(target)) {
            PENDING.remove(player.getUniqueId());
            ArenaSlotSounds.play(player, ArenaSlotSounds.Kind.CONFIRM);
            flash(match, target, BattleArena.PedalHighlight.CONFIRMED, BattleArena.PedalHighlight.NORMAL);
            ShopVillagerService.tryOpenShop(player, match);
            return true;
        }
        clearPending(player.getUniqueId());
        select(player, match, target);
        match.feedback().actionBarInfo("<yellow>已选中商店，再次左键确认打开");
        return true;
    }

    private static boolean handleCardPlay(
            Player player, InscriptionMatch match, MatchSide side, ArenaPedalTarget target, ItemStack hand) {
        if (!(target instanceof ArenaPedalTarget.Battle battle)) {
            return false;
        }
        if (BoardSides.toSlotOwner(side) != battle.owner()) {
            warn(player, match, "<yellow>只能在自己站场区出牌");
            return true;
        }
        if (match.isCombatAnimating()) {
            warn(player, match, "<yellow>战斗进行中");
            return true;
        }
        if (match.turn().phase() != TurnPhase.PLAY || side != match.actingSide()) {
            warn(player, match, "<yellow>当前无法出牌");
            return true;
        }

        String templateId = cardTemplateId(hand);
        if (templateId == null) {
            return false;
        }

        PendingSelection pending = PENDING.get(player.getUniqueId());
        if (pending != null && pending.sameTarget(target)) {
            PENDING.remove(player.getUniqueId());
            ArenaSlotSounds.play(player, ArenaSlotSounds.Kind.CONFIRM);
            flash(match, target, BattleArena.PedalHighlight.CONFIRMED, BattleArena.PedalHighlight.NORMAL);
            match.playCardToSlot(templateId, battle.index(), side, null);
            return true;
        }

        clearPending(player.getUniqueId());
        select(player, match, target);
        match.feedback().actionBarInfo("<yellow>已选中槽位 %d，再次左键确认出牌".formatted(battle.index() + 1));
        return true;
    }

    private static boolean handleSacrificeOrBeam(
            Player player, InscriptionMatch match, MatchSide side, ArenaPedalTarget target) {
        if (!(target instanceof ArenaPedalTarget.Battle battle)) {
            return false;
        }
        if (BoardSides.toSlotOwner(side) != battle.owner()) {
            warn(player, match, "<yellow>只能献祭己方造物");
            return true;
        }
        BoardSlot slot = match.board().slot(battle.owner(), battle.index());
        if (slot.isEmpty()) {
            warn(player, match, "<yellow>该槽位没有可献祭的造物");
            return true;
        }
        BoardCreature creature = slot.creature();
        if (creature == null) {
            return true;
        }

        PendingSelection pending = PENDING.get(player.getUniqueId());
        if (pending != null && pending.sameTarget(target)) {
            PENDING.remove(player.getUniqueId());
            ArenaSlotSounds.play(player, ArenaSlotSounds.Kind.CONFIRM);
            flash(match, target, BattleArena.PedalHighlight.CONFIRMED, BattleArena.PedalHighlight.NORMAL);
            int value = creature.definition().sacrificeValue();
            if (value >= 2) {
                new AlertMenu(player, () -> match.sacrifice(creature, side)).open();
            } else {
                match.sacrifice(creature, side);
            }
            return true;
        }

        clearPending(player.getUniqueId());
        select(player, match, target);
        match.feedback().actionBarInfo("<yellow>已选中 <white>%s<yellow>，再次左键确认献祭".formatted(creature.displayName()));
        return true;
    }

    private static void select(Player player, InscriptionMatch match, ArenaPedalTarget target) {
        PENDING.put(playerId(player), new PendingSelection(match, target));
        match.arena().setPedalHighlight(target, BattleArena.PedalHighlight.SELECTED);
        ArenaSlotSounds.play(player, ArenaSlotSounds.Kind.SELECT);
    }

    private static void warn(Player player, InscriptionMatch match, String message) {
        match.feedback().actionBarWarn(message);
        ArenaSlotSounds.play(player, ArenaSlotSounds.Kind.REJECT);
    }

    private static UUID playerId(Player player) {
        return player.getUniqueId();
    }

    private static void clearPending(UUID playerId) {
        PendingSelection pending = PENDING.remove(playerId);
        if (pending != null && pending.match().arena() != null) {
            pending.match().arena().setPedalHighlight(pending.target(), BattleArena.PedalHighlight.NORMAL);
        }
    }

    private static void flash(
            InscriptionMatch match,
            ArenaPedalTarget target,
            BattleArena.PedalHighlight flash,
            BattleArena.PedalHighlight restore) {
        match.arena().flashPedal(target, flash, restore, FLASH_TICKS);
    }

    private static boolean isEmptyHand(ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    private static boolean isCard(ItemStack stack) {
        if (isEmptyHand(stack)) {
            return false;
        }
        MGCItem mgc = MCZJUGameCore.getItemManager().get(stack);
        return mgc instanceof InscriptionCardItem;
    }

    private static String cardTemplateId(ItemStack stack) {
        MGCItem mgc = MCZJUGameCore.getItemManager().get(stack);
        if (mgc instanceof InscriptionCardItem card) {
            return card.templateId();
        }
        return null;
    }

    private static boolean isSacrificeSword(ItemStack stack) {
        return stack != null
                && (InscriptionItems.sacrificeSword().isThis(stack)
                        || InscriptionItemUtil.isTool(stack, InscriptionItems.sacrificeSword()));
    }

    private record PendingSelection(InscriptionMatch match, ArenaPedalTarget target) {

        boolean sameTarget(ArenaPedalTarget other) {
            return Objects.equals(target, other);
        }
    }
}
