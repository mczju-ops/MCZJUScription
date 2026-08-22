package com.github.mczju.mczjuscription.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class InscriptionKeys {

    public static NamespacedKey SLOT_OWNER;
    public static NamespacedKey SLOT_INDEX;
    public static NamespacedKey ARENA_ID;
    public static NamespacedKey CREATURE_INSTANCE;
    public static NamespacedKey RESOURCE_ITEM;
    public static NamespacedKey DROP_CARD;
    public static NamespacedKey DROP_PLAYER;
    /** 大厅座位范围示意 BlockDisplay */
    public static NamespacedKey HUB_SEAT_MARKER;
    /** 对局内土豆等展示模型 */
    public static NamespacedKey MATCH_DISPLAY;
    /** 流浪商人实体（含展示实体保底） */
    public static NamespacedKey WANDERING_TRADER;
    /** 商店模式常驻村民 */
    public static NamespacedKey SHOP_VILLAGER;
    /** 商店村民所属阵营 */
    public static NamespacedKey SHOP_VILLAGER_SIDE;
    /** 兔子堆箱内待领取的展示兔 */
    public static NamespacedKey RABBIT_CHEST;
    /** 兔子堆箱所属阵营 */
    public static NamespacedKey RABBIT_CHEST_SIDE;
    /** UI 槽位所属阵营（PLAYER / ENEMY） */
    public static NamespacedKey UI_SLOT_SIDE;
    /** 踏板高亮状态：normal / selected / confirmed */
    public static NamespacedKey PEDAL_HIGHLIGHT;
    /** 献祭等纯展示用掉落物，不可拾取 */
    public static NamespacedKey COSMETIC_DROP;

    private InscriptionKeys() {}

    public static void init(Plugin plugin) {
        SLOT_OWNER = new NamespacedKey(plugin, "slot_owner");
        SLOT_INDEX = new NamespacedKey(plugin, "slot_index");
        ARENA_ID = new NamespacedKey(plugin, "arena_id");
        CREATURE_INSTANCE = new NamespacedKey(plugin, "creature_instance");
        RESOURCE_ITEM = new NamespacedKey(plugin, "resource_item");
        DROP_CARD = new NamespacedKey(plugin, "drop_card");
        DROP_PLAYER = new NamespacedKey(plugin, "drop_player");
        HUB_SEAT_MARKER = new NamespacedKey(plugin, "hub_seat_marker");
        MATCH_DISPLAY = new NamespacedKey(plugin, "match_display");
        WANDERING_TRADER = new NamespacedKey(plugin, "wandering_trader");
        SHOP_VILLAGER = new NamespacedKey(plugin, "shop_villager");
        SHOP_VILLAGER_SIDE = new NamespacedKey(plugin, "shop_villager_side");
        RABBIT_CHEST = new NamespacedKey(plugin, "rabbit_chest");
        RABBIT_CHEST_SIDE = new NamespacedKey(plugin, "rabbit_chest_side");
        UI_SLOT_SIDE = new NamespacedKey(plugin, "ui_slot_side");
        PEDAL_HIGHLIGHT = new NamespacedKey(plugin, "pedal_highlight");
        COSMETIC_DROP = new NamespacedKey(plugin, "cosmetic_drop");
    }
}
