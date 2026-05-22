package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 查看当前编辑中的牌组：相同卡堆叠显示，丢弃键移除一张。 */
public final class DeckViewMenu extends Menu {

  public static final int BACK_SLOT = 49;
  private static final int CONTENT_SLOTS = 45;

  private final List<String> editing;
  private final int returnPage;
  private final Map<Integer, String> slotToCardId = new LinkedHashMap<>();

  DeckViewMenu(Player player, List<String> editing, int returnPage) {
    super(player);
    this.editing = editing;
    this.returnPage = Math.max(0, returnPage);
  }

  List<String> editing() {
    return editing;
  }

  int returnPage() {
    return returnPage;
  }

  public String cardIdAtSlot(int rawSlot) {
    return slotToCardId.get(rawSlot);
  }

  public void removeOne(String templateId) {
    int index = editing.indexOf(templateId);
    if (index >= 0) {
      editing.remove(index);
    }
    reloadView();
  }

  public void reloadView() {
    setup();
    player.player().updateInventory();
  }

  @Override
  protected void setup() {
    inventory.clear();
    slotToCardId.clear();

    List<Map.Entry<String, Integer>> stacks = stackCounts(editing);
    int slot = 0;
    for (Map.Entry<String, Integer> entry : stacks) {
      if (slot >= CONTENT_SLOTS) break;
      String id = entry.getKey();
      int count = entry.getValue();
      CardTemplate template = CardCatalog.require(id);
      org.bukkit.Material icon =
          "mob_fish_dried".equals(id) ? org.bukkit.Material.COD : template.spawnEggMaterial();
      slotToCardId.put(slot, id);
      setSlot(
          slot++,
          ItemBuilder.of(icon)
              .customName("<white>" + template.displayName())
              .amount(Math.min(64, Math.max(1, count)))
              .lore(
                  List.of(
                      "<gray>数量: <white>" + count,
                      "<gray>ID: <dark_gray>" + id,
                      "<yellow>按 Q 丢弃键移除 1 张"))
              .build());
    }

    setSlot(
        BACK_SLOT,
        ItemBuilder.of(org.bukkit.Material.ARROW)
            .customName("<gray>返回构牌")
            .lore(List.of("<gray>共 " + editing.size() + " 张"))
            .build(),
        (p, e) -> new DeckBuilderMenu(p.player(), editing, returnPage).open());
  }

  static List<Map.Entry<String, Integer>> stackCounts(List<String> deck) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String id : deck) {
      counts.merge(id, 1, Integer::sum);
    }
    return new ArrayList<>(counts.entrySet());
  }

  @Override
  protected String getTitle() {
    return "当前牌组 · " + editing.size() + " 张";
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.deck";
  }
}
