package com.github.mczju.mczjuscription.item;

import com.github.mczju.mczjuscription.game.card.CardDefinition;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.card.CardRegistry;
import com.github.mczju.mczjuscription.game.card.CostType;
import com.github.mczjuops.mczjugamecore.item.MGCItem;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class InscriptionCardItem extends MGCItem {

    private final CardId cardId;
    private final String itemId;

    public InscriptionCardItem(CardId cardId) {
        this.cardId = cardId;
        this.itemId = InscriptionItems.cardItemId(cardId);
    }

    @Override
    public String getId() {
        return itemId;
    }

    public CardId cardId() {
        return cardId;
    }

    @Override
    protected ItemStack createRawItem() {
        CardDefinition def = CardRegistry.get(cardId);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>力量 %d  |  生命 %d".formatted(def.power(), def.health()));
        lore.add(costLine(def));
        if (!def.sigils().isEmpty()) {
            lore.add("<dark_purple>印记: " + def.sigils());
        }
        lore.add("<dark_gray>丢弃到己方槽位方块上召唤");
        return ItemBuilder.of(def.spawnEggMaterial())
                .customName("<white>" + def.displayName())
                .lore(lore)
                .maxStackSize(16)
                .build();
    }

    private static String costLine(CardDefinition def) {
        return switch (def.costType()) {
            case FREE -> "<green>花费：免费";
            case BLOOD -> "<red>花费：腐肉 ×%d".formatted(def.cost());
            case BONES -> "<gold>花费：骨币 ×%d".formatted(def.cost());
        };
    }
}
