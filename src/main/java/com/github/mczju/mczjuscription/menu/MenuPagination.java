package com.github.mczju.mczjuscription.menu;

import com.github.mczjuops.mczjugamecore.menu.SlotAction;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import java.util.function.IntConsumer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** 菜单列表翻页：内容区 0–44，右下角 52/53 固定为上一页/下一页。 */
public final class MenuPagination {

  public static final int CONTENT_SLOTS = 45;
  public static final int PREV_SLOT = 52;
  public static final int NEXT_SLOT = 53;

  @FunctionalInterface
  public interface SlotBinder {
    void set(int slot, ItemStack item, SlotAction action);
  }

  private MenuPagination() {}

  public static int totalPages(int itemCount) {
    return Math.max(1, (itemCount + CONTENT_SLOTS - 1) / CONTENT_SLOTS);
  }

  public static int clampPage(int page, int itemCount) {
    return Math.min(Math.max(0, page), totalPages(itemCount) - 1);
  }

  public static int rangeStart(int page) {
    return page * CONTENT_SLOTS;
  }

  public static int rangeEnd(int page, int itemCount) {
    return Math.min(rangeStart(page) + CONTENT_SLOTS, itemCount);
  }

  public static String titleSuffix(int page, int itemCount) {
    int total = totalPages(itemCount);
    return " · " + (clampPage(page, itemCount) + 1) + "/" + total;
  }

  /** 在右下角放置上一页 / 下一页箭头（始终显示，不可用时为灰色）。 */
  public static void placeCornerArrows(
      SlotBinder binder, int currentPage, int itemCount, IntConsumer onPageChange) {
    int totalPages = totalPages(itemCount);
    boolean hasPrev = currentPage > 0;
    boolean hasNext = currentPage < totalPages - 1;

    binder.set(
        PREV_SLOT,
        arrowItem(hasPrev, true, currentPage, totalPages),
        hasPrev ? (p, e) -> onPageChange.accept(currentPage - 1) : (p, e) -> {});

    binder.set(
        NEXT_SLOT,
        arrowItem(hasNext, false, currentPage, totalPages),
        hasNext ? (p, e) -> onPageChange.accept(currentPage + 1) : (p, e) -> {});
  }

  private static ItemStack arrowItem(
      boolean enabled, boolean prev, int currentPage, int totalPages) {
    String label = prev ? "上一页" : "下一页";
    if (!enabled) {
      return ItemBuilder.of(Material.ARROW)
          .customName("<dark_gray>" + label)
          .lore(List.of("<dark_gray>已是" + (prev ? "第一" : "最后一") + "页"))
          .build();
    }
    int target = prev ? currentPage : currentPage + 2;
    return ItemBuilder.of(Material.ARROW)
        .customName("<yellow>" + label)
        .lore(List.of("<gray>第 " + target + " / " + totalPages + " 页"))
        .build();
  }
}
