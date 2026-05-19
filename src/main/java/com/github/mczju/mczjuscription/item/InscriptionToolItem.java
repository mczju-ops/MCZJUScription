package com.github.mczju.mczjuscription.item;

import com.github.mczjuops.mczjugamecore.item.MGCItem;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class InscriptionToolItem extends MGCItem {

    private final String id;
    private final Material material;
    private final String name;
    private final List<String> lore;

    public InscriptionToolItem(String id, Material material, String name, List<String> lore) {
        this.id = id;
        this.material = material;
        this.name = name;
        this.lore = lore;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    protected ItemStack createRawItem() {
        return ItemBuilder.of(material)
                .customName(name)
                .lore(lore)
                .maxStackSize(1)
                .build();
    }
}
