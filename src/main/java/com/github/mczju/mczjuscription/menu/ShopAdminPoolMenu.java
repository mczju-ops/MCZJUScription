package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import com.github.mczju.mczjuscription.shop.ShopPoolEntry;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 管理员：刷新池（每回合 4 格从此池加权抽取）。 */
public final class ShopAdminPoolMenu extends Menu {

  public static final int ADD_SLOT = 49;
  private static final int MAX_LIST = 45;

  private int selectedIndex = -1;

  public ShopAdminPoolMenu(Player player) {
    super(player);
  }

  public void setSelectedIndex(int selectedIndex) {
    this.selectedIndex = selectedIndex;
  }

  public int selectedIndex() {
    return selectedIndex;
  }

  public List<ShopPoolEntry> pool() {
    return ShopConfigStorage.get().rotatingPool();
  }

  @Override
  protected void setup() {
    inventory.clear();
    List<ShopPoolEntry> pool = pool();
    for (int i = 0; i < pool.size() && i < MAX_LIST; i++) {
      int index = i;
      ShopPoolEntry entry = pool.get(i);
      CardTemplate def = CardCatalog.require(entry.templateId());
      boolean selected = index == selectedIndex;
      setSlot(
          i,
          ItemBuilder.of(def.spawnEggMaterial())
              .customName((selected ? "<green>✓ " : "") + def.displayName())
              .lore(
                  List.of(
                      "<gray>ID: " + entry.templateId(),
                      "<gold>骨币: " + entry.priceBones(),
                      "<aqua>权重: " + entry.weight(),
                      "<gray>左键价+1 右键价-1",
                      "<aqua>Shift+左键 对话框输入权重",
                      "<red>丢弃键删除条目"))
              .build(),
          (p, e) -> {
            if (e.isShiftClick()) {
              return;
            }
            selectedIndex = index;
            ShopPoolEntry cur = pool.get(index);
            if (e.isLeftClick()) {
              pool.set(
                  index,
                  new ShopPoolEntry(cur.templateId(), cur.priceBones() + 1, cur.weight()));
            } else if (e.isRightClick()) {
              pool.set(
                  index,
                  new ShopPoolEntry(
                      cur.templateId(), Math.max(0, cur.priceBones() - 1), cur.weight()));
            }
            refresh();
          });
    }

    setSlot(
        ADD_SLOT,
        ItemBuilder.of(Material.EMERALD)
            .customName("<green>添加卡牌")
            .lore(
                List.of(
                    "<yellow>手持邪恶冥刻卡牌点击本格",
                    "<gray>默认骨币 2 · 权重 10"))
            .build(),
        (p, e) -> {});

    if (selectedIndex >= 0 && selectedIndex < pool.size()) {
      ShopPoolEntry sel = pool.get(selectedIndex);
      setSlot(
          45,
          ItemBuilder.of(Material.GOLD_NUGGET)
              .customName("<gold>选中: " + sel.templateId())
              .lore(List.of("<gray>骨币 " + sel.priceBones() + " · 权重 " + sel.weight()))
              .build(),
          (p, e) -> {});
    }

    setSlot(
        53,
        ItemBuilder.of(Material.ARROW).customName("<gray>返回").build(),
        (p, e) -> new ShopAdminMenu(p.player()).open());
  }

  public void addFromHand(String templateId) {
    pool().add(new ShopPoolEntry(templateId, 2, 10));
    refresh();
  }

  public void removeAt(int index) {
    List<ShopPoolEntry> pool = pool();
    if (index >= 0 && index < pool.size()) {
      pool.remove(index);
      selectedIndex = -1;
      refresh();
    }
  }

  public void refresh() {
    setup();
  }

  @Override
  protected String getTitle() {
    return "刷新池";
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.admin";
  }
}
