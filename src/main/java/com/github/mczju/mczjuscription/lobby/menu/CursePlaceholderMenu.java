package com.github.mczju.mczjuscription.lobby.menu;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 诅咒选择（占位选项，写入玩家数据供对局/排行使用）。 */
public final class CursePlaceholderMenu extends Menu {

    private static final String[][] CURSES = {
        {"none", "无", "<gray>标准难度"},
        {"glass", "玻璃之魂", "<dark_aqua>占位 · 更高难度分"},
        {"ring", "戒环", "<dark_red>占位 · 更高难度分"}
    };

    public CursePlaceholderMenu(Player player) {
        super(player);
    }

    @Override
    protected void setup() {
        inventory.clear();
        InscriptionPlayerData data =
                player.getData(InscriptionPlayerData.class);
        String current = data.selectedCurse == null ? "none" : data.selectedCurse;

        int slot = 10;
        for (String[] curse : CURSES) {
            boolean selected = curse[0].equalsIgnoreCase(current);
            setSlot(
                    slot,
                    ItemBuilder.of(selected ? Material.ENCHANTED_BOOK : Material.BOOK)
                            .customName((selected ? "<green>✓ " : "") + "<white>" + curse[1])
                            .lore(List.of(curse[2], "<yellow>点击选择"))
                            .build(),
                    (p, e) -> {
                        data.selectedCurse = curse[0];
                        data.setModified(true);
                        p.sender().success("已选择诅咒：<white>%s".formatted(curse[1]));
                        setup();
                    });
            slot += 2;
        }
    }

    @Override
    protected String getTitle() {
        return "诅咒选择";
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
