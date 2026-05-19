package com.github.mczju.mczjuscription.lobby.menu;

import com.github.mczju.mczjuscription.lobby.LeaderboardService;
import com.github.mczju.mczjuscription.lobby.LeaderboardService.LeaderboardEntry;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 通关排行榜前十。 */
public final class LeaderboardMenu extends Menu {

    public LeaderboardMenu(Player player) {
        super(player);
    }

    @Override
    protected void setup() {
        inventory.clear();
        List<LeaderboardEntry> top = LeaderboardService.topEntries();
        if (top.isEmpty()) {
            setSlot(
                    13,
                    ItemBuilder.of(Material.PAPER)
                            .customName("<gray>暂无记录")
                            .lore(List.of("<gray>完成一局后会出现在此"))
                            .build());
            return;
        }
        int slot = 0;
        int rank = 1;
        for (LeaderboardEntry entry : top) {
            if (slot >= 45) {
                break;
            }
            Material icon =
                    rank == 1
                            ? Material.GOLD_BLOCK
                            : rank <= 3 ? Material.IRON_BLOCK : Material.STONE;
            setSlot(
                    slot++,
                    ItemBuilder.of(icon)
                            .customName(
                                    "<gold>#%d <white>%s"
                                            .formatted(rank, entry.displayName()))
                            .lore(
                                    List.of(
                                            "<gray>难度 <white>%d".formatted(entry.difficulty()),
                                            "<gray>该难度通关 <white>%d 次".formatted(entry.clears())))
                            .build());
            rank++;
        }
    }

    @Override
    protected String getTitle() {
        return "通关排行榜";
    }

    @Override
    protected @Range(from = 1, to = 6) int getRows() {
        return 6;
    }

    @Override
    protected String getPermission() {
        return "inscription.hub";
    }
}
