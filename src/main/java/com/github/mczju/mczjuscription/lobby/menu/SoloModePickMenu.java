package com.github.mczju.mczjuscription.lobby.menu;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczju.mczjuscription.lobby.HubMatchLauncher;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 单人座位：选择商店或自由构牌。 */
public final class SoloModePickMenu extends Menu {

    public SoloModePickMenu(Player player) {
        super(player);
    }

    @Override
    protected void setup() {
        inventory.clear();
        setSlot(
                11,
                ItemBuilder.of(Material.EMERALD)
                        .customName("<gold>商店模式")
                        .lore(
                                List.of(
                                        "<gray>每回合商店购卡",
                                        "<yellow>点击开始"))
                        .build(),
                (p, e) -> {
                    p.player().closeInventory();
                    HubMatchLauncher.startSolo(p, PlayVariant.SOLO_SHOP);
                });

        setSlot(
                15,
                ItemBuilder.of(Material.WRITABLE_BOOK)
                        .customName("<aqua>自由组卡")
                        .lore(
                                List.of(
                                        "<gray>使用已保存牌组",
                                        "<yellow>点击开始"))
                        .build(),
                (p, e) -> {
                    p.player().closeInventory();
                    HubMatchLauncher.startSolo(p, PlayVariant.SOLO_FREE_BUILD);
                });

        setSlot(
                22,
                ItemBuilder.of(Material.BARRIER).customName("<red>离开座位").build(),
                (p, e) -> p.player().closeInventory());
    }

    @Override
    protected String getTitle() {
        return "单人 · 选择模式";
    }

    @Override
    protected @Range(from = 1, to = 6) int getRows() {
        return 3;
    }

    @Override
    protected String getPermission() {
        return "inscription.hub";
    }
}
