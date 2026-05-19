package com.github.mczju.mczjuscription.item;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.ui.MatchHotbar;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.item.ItemManager;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public final class InscriptionItems {

  public static final String ID_PREFIX = "inscription:";
  private static final String PREFIX = ID_PREFIX;
  private static final Map<String, InscriptionCardItem> CARDS = new ConcurrentHashMap<>();

  private static InscriptionToolItem sacrificeSword;
  private static InscriptionToolItem drawDeck;
  private static InscriptionToolItem shopDeck;
  private static InscriptionToolItem rabbitPile;

  private InscriptionItems() {}

  public static void registerAll() {
    for (CardId id : CardId.values()) {
      registerCard(id.name());
    }
    sacrificeSword =
        new InscriptionToolItem(
            PREFIX + "sacrifice_sword",
            Material.IRON_SWORD,
            "<red>献祭之剑",
            List.of("<gray>右键己方造物进行献祭", "<gray>获得腐肉（鲜血）"));
    drawDeck =
        new InscriptionToolItem(
            PREFIX + "deck",
            Material.BOOK,
            "<aqua>主牌组",
            List.of("<gray>右键从主牌组抽一张牌"));
    shopDeck =
        new InscriptionToolItem(
            PREFIX + "shop",
            Material.EMERALD,
            "<gold>商店",
            List.of("<gray>抽牌阶段右键反复打开购卡", "<gray>潜行+右键结束购牌进入出牌"));
    rabbitPile =
        new InscriptionToolItem(
            PREFIX + "rabbit_pile",
            Material.CHEST,
            "<gold>兔子堆",
            List.of("<gray>每回合一次，抽一张免费兔子"));
    register(sacrificeSword);
    register(drawDeck);
    register(shopDeck);
    register(rabbitPile);
  }

  private static void register(MGCItem item) {
    MCZJUGameCore.getItemManager().register(item);
  }

  public static void registerCard(String templateId) {
    InscriptionCardItem item = new InscriptionCardItem(templateId);
    CARDS.put(templateId, item);
    register(item);
  }

  public static String cardItemId(String templateId) {
    return PREFIX + "card:" + templateId.toLowerCase();
  }

  public static String cardItemId(CardId cardId) {
    return cardItemId(cardId.name());
  }

  public static String parseTemplateId(String itemId) {
    if (itemId == null || !itemId.startsWith(PREFIX + "card:")) return null;
    String slug = itemId.substring((PREFIX + "card:").length());
    for (String known : CardCatalog.all().stream().map(t -> t.id()).toList()) {
      if (known.equalsIgnoreCase(slug)) return known;
    }
    return slug.toUpperCase();
  }

  public static InscriptionCardItem card(String templateId) {
    return CARDS.computeIfAbsent(
        templateId,
        id -> {
          InscriptionCardItem created = new InscriptionCardItem(id);
          register(created);
          return created;
        });
  }

  public static InscriptionCardItem card(CardId cardId) {
    return card(cardId.name());
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

  public static void stripPlayerInventory(org.bukkit.entity.Player player) {
    com.github.mczju.mczjuscription.ui.ResourceHotbar.clear(player);
    InscriptionItemUtil.clearAllInscriptionItems(player);
  }

  public static void syncHandItems(org.bukkit.entity.Player player, List<String> hand) {
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
    for (String templateId : hand) {
      player.getInventory().addItem(card(templateId).getItem());
    }
  }

  public static void giveStarterKit(PlayerExt player, DeckMode deckMode) {
    stripPlayerInventory(player.player());
    MatchHotbar.placeTools(player.player(), deckMode);
  }
}
