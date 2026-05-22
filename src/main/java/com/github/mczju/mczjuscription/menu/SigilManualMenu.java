package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.sigil.SigilDescriptions;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 对局内印记说明书：分页浏览全部印记效果。 */
public final class SigilManualMenu extends Menu {

  private final int page;

  public SigilManualMenu(Player player, int page) {
    super(player);
    this.page = Math.max(0, page);
  }

  @Override
  protected void setup() {
    inventory.clear();
    List<SigilId> sigils = SigilDescriptions.allSortedByDisplayName();
    int currentPage = MenuPagination.clampPage(page, sigils.size());
    int start = MenuPagination.rangeStart(currentPage);
    int end = MenuPagination.rangeEnd(currentPage, sigils.size());

    int slot = 0;
    for (int i = start; i < end; i++) {
      SigilId sigil = sigils.get(i);
      boolean implemented = SigilDescriptions.isImplemented(sigil);
      List<String> lore = new ArrayList<>(SigilDescriptions.loreLines(sigil));
      if (!implemented) {
        lore.add("<red>战斗逻辑尚未接入");
      }
      setSlot(
          slot++,
          ItemBuilder.of(implemented ? Material.BOOK : Material.GRAY_DYE)
              .customName(
                  "<light_purple>"
                      + SigilNames.display(sigil)
                      + (implemented ? "" : " <dark_gray>(未接)"))
              .lore(lore)
              .build());
    }

    setSlot(
        49,
        ItemBuilder.of(Material.BARRIER)
            .customName("<gray>关闭")
            .lore(List.of("<gray>返回对局"))
            .build(),
        (p, e) -> p.player().closeInventory());

    MenuPagination.placeCornerArrows(
        this::setSlot, currentPage, sigils.size(), nextPage -> new SigilManualMenu(player.player(), nextPage).open());
  }

  @Override
  protected String getTitle() {
    return "印记说明书"
        + MenuPagination.titleSuffix(page, SigilDescriptions.allSortedByDisplayName().size());
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.play";
  }
}
