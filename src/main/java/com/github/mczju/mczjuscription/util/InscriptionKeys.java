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

    private InscriptionKeys() {}

    public static void init(Plugin plugin) {
        SLOT_OWNER = new NamespacedKey(plugin, "slot_owner");
        SLOT_INDEX = new NamespacedKey(plugin, "slot_index");
        ARENA_ID = new NamespacedKey(plugin, "arena_id");
        CREATURE_INSTANCE = new NamespacedKey(plugin, "creature_instance");
        RESOURCE_ITEM = new NamespacedKey(plugin, "resource_item");
        DROP_CARD = new NamespacedKey(plugin, "drop_card");
        DROP_PLAYER = new NamespacedKey(plugin, "drop_player");
    }
}
