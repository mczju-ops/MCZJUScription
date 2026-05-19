package com.github.mczju.mczjuscription.lobby.menu;

import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 赐福区占位（局外养成待接）。 */
public final class BlessingPlaceholderMenu extends Menu {

    public BlessingPlaceholderMenu(Player player) {
        super(player);
    }

    @Override
    protected void setup() {
        inventory.clear();
        setSlot(
                13,
                ItemBuilder.of(Material.ENCHANTED_GOLDEN_APPLE)
                        .customName("<light_purple>赐福")
                        .lore(
                                List.of(
                                        "<gray>局外永久增益系统",
                                        "<yellow>即将推出",
                                        "<dark_gray>敬请期待"))
                        .build());
    }

    @Override
    protected String getTitle() {
        return "赐福";
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
