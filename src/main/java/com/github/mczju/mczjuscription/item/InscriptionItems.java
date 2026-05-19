package com.github.mczju.mczjuscription.item;

import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.ui.MatchHotbar;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.item.ItemManager;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class InscriptionItems {

    public static final String ID_PREFIX = "inscription:";
    private static final String PREFIX = ID_PREFIX;
    private static final Map<CardId, InscriptionCardItem> CARDS = new EnumMap<>(CardId.class);

    private static InscriptionToolItem sacrificeSword;
    private static InscriptionToolItem drawDeck;
    private static InscriptionToolItem shopDeck;
    private static InscriptionToolItem rabbitPile;

    private InscriptionItems() {}

    public static void registerAll() {
        for (CardId id : CardId.values()) {
            InscriptionCardItem item = new InscriptionCardItem(id);
            CARDS.put(id, item);
            register(item);
        }
        sacrificeSword = new InscriptionToolItem(
                PREFIX + "sacrifice_sword",
                Material.IRON_SWORD,
                "<red>献祭之剑",
                List.of("<gray>右键己方造物进行献祭", "<gray>获得腐肉（鲜血）")
        );
        drawDeck = new InscriptionToolItem(
                PREFIX + "deck",
                Material.BOOK,
                "<aqua>主牌组",
                List.of("<gray>右键从主牌组抽一张牌")
        );
        shopDeck = new InscriptionToolItem(
                PREFIX + "shop",
                Material.EMERALD,
                "<gold>商店",
                List.of("<gray>抽牌阶段右键反复打开购卡", "<gray>潜行+右键结束购牌进入出牌")
        );
        rabbitPile = new InscriptionToolItem(
                PREFIX + "rabbit_pile",
                Material.CHEST,
                "<gold>兔子堆",
                List.of("<gray>每回合一次，抽一张免费兔子")
        );
        register(sacrificeSword);
        register(drawDeck);
        register(shopDeck);
        register(rabbitPile);
    }

    private static void register(MGCItem item) {
        MCZJUGameCore.getItemManager().register(item);
    }

    public static String cardItemId(CardId cardId) {
        return PREFIX + "card:" + cardId.name().toLowerCase();
    }

    public static InscriptionCardItem card(CardId cardId) {
        return CARDS.get(cardId);
    }

    public static InscriptionToolItem sacrificeSword() {
        return sacrificeSword;
    }

    public static InscriptionToolItem drawDeck() {
        return drawDeck;
    }

    public static InscriptionToolItem shopDeck() {
        return shopDeck;
    }

    public static InscriptionToolItem rabbitPile() {
        return rabbitPile;
    }

    /** 清空背包中的本局卡牌/工具/资源显示物（不触碰逻辑手牌列表，由对局状态另行 clear）。 */
    public static void stripPlayerInventory(org.bukkit.entity.Player player) {
        com.github.mczju.mczjuscription.ui.ResourceHotbar.clear(player);
        InscriptionItemUtil.clearAllInscriptionItems(player);
    }

    /**
     * 按逻辑手牌重新发放卡牌物品（先清空所有卡物品，再逐张给予）。
     */
    public static void syncHandItems(org.bukkit.entity.Player player, java.util.List<CardId> hand) {
        ItemManager manager = MCZJUGameCore.getItemManager();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (MatchHotbar.isReservedToolSlot(i) || MatchHotbar.isResourceSlot(i)) {
                continue;
            }
            ItemStack stack = inv.getItem(i);
            if (stack == null) continue;
            String id = manager.getItemId(stack);
            if (id != null && id.startsWith(PREFIX + "card:")) {
                inv.setItem(i, null);
            }
        }
        for (CardId cardId : hand) {
            player.getInventory().addItem(card(cardId).getItem());
        }
    }

    /**
     * 每局开局强制刷新工具（避免 profile 里残留无 PDC 的原版物品导致右键无反应）。
     */
    public static void giveStarterKit(PlayerExt player, DeckMode deckMode) {
        stripPlayerInventory(player.player());
        MatchHotbar.placeTools(player.player(), deckMode);
    }
}
