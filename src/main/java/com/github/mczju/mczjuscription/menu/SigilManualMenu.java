package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.sigil.SigilDescriptions;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczju.mczjuscription.ui.DialogTextInput;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 对局内印记说明书：分页浏览全部印记效果。 */
public final class SigilManualMenu extends Menu {

  public static final int SEARCH_SLOT = 45;

  private final int page;
  private final String searchQuery;

  public SigilManualMenu(Player player, int page) {
    this(player, page, null);
  }

  public SigilManualMenu(Player player, int page, String searchQuery) {
    super(player);
    this.page = Math.max(0, page);
    this.searchQuery = normalizeQuery(searchQuery);
  }

  @Override
  protected void setup() {
    inventory.clear();
    List<SigilId> sigils = SigilDescriptions.filteredSorted(searchQuery);
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

    placeSearchButton(currentPage);

    setSlot(
        49,
        ItemBuilder.of(Material.BARRIER)
            .customName("<gray>关闭")
            .lore(List.of("<gray>返回对局"))
            .build(),
        (p, e) -> p.player().closeInventory());

    MenuPagination.placeCornerArrows(
        this::setSlot,
        currentPage,
        sigils.size(),
        nextPage -> new SigilManualMenu(player.player(), nextPage, searchQuery).open());
  }

  private void placeSearchButton(int currentPage) {
    List<String> lore = new ArrayList<>();
    if (searchQuery != null) {
      lore.add("<gray>当前：<white>" + searchQuery);
      lore.add("<gray>按 Q 丢弃此按钮可清除筛选");
    } else {
      lore.add("<gray>按名称或效果关键词筛选");
    }
    lore.add("<yellow>点击输入搜索词");
    setSlot(
        SEARCH_SLOT,
        ItemBuilder.of(Material.SPYGLASS)
            .customName("<aqua>搜索印记")
            .lore(lore)
            .build(),
        (p, e) -> DialogTextInput.openSigilSearch(p.player(), currentPage, searchQuery));
  }

  public boolean hasActiveSearch() {
    return searchQuery != null;
  }

  public void clearSearch() {
    new SigilManualMenu(player.player(), 0, null).open();
  }

  @Override
  protected String getTitle() {
    List<SigilId> sigils = SigilDescriptions.filteredSorted(searchQuery);
    String base = "印记说明书";
    if (searchQuery != null) {
      base += " · " + searchQuery;
    }
    return base + MenuPagination.titleSuffix(page, sigils.size());
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.play";
  }

  private static String normalizeQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }
    return query.trim();
  }
}
