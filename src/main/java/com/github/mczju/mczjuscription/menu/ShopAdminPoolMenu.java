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

  public static final int ADD_SLOT = 45;

  private int selectedIndex = -1;
  private final int page;

  public ShopAdminPoolMenu(Player player) {
    this(player, 0);
  }

  public ShopAdminPoolMenu(Player player, int page) {
    super(player);
    this.page = Math.max(0, page);
  }

  public void setSelectedIndex(int selectedIndex) {
    this.selectedIndex = selectedIndex;
  }

  public int selectedIndex() {
    return selectedIndex;
  }

  public int page() {
    return page;
  }

  public List<ShopPoolEntry> pool() {
    return ShopConfigStorage.get().rotatingPool();
  }

  /** 内容格 rawSlot → 刷新池全局下标，无效时 -1。 */
  public int poolIndexAt(int contentSlot) {
    if (contentSlot < 0 || contentSlot >= MenuPagination.CONTENT_SLOTS) {
      return -1;
    }
    int index = MenuPagination.rangeStart(page) + contentSlot;
    return index < pool().size() ? index : -1;
  }

  @Override
  protected void setup() {
    inventory.clear();
    List<ShopPoolEntry> pool = pool();
    int currentPage = MenuPagination.clampPage(page, pool.size());
    int start = MenuPagination.rangeStart(currentPage);
    int end = MenuPagination.rangeEnd(currentPage, pool.size());

    int contentSlot = 0;
    for (int i = start; i < end; i++) {
      int index = i;
      ShopPoolEntry entry = pool.get(i);
      CardTemplate def = CardCatalog.require(entry.templateId());
      boolean selected = index == selectedIndex;
      setSlot(
          contentSlot++,
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
                    "<yellow>或点击下方从卡牌库选择",
                    "<gray>默认骨币 2 · 权重 10"))
            .build(),
        (p, e) -> {});

    setSlot(
        46,
        ItemBuilder.of(Material.CHEST)
            .customName("<aqua>从卡牌库选择")
            .lore(List.of("<gray>分页浏览全部卡牌并加入刷新池"))
            .build(),
        (p, e) -> new ShopAdminPoolAddMenu(p.player(), this, 0).open());

    if (selectedIndex >= 0 && selectedIndex < pool.size()) {
      ShopPoolEntry sel = pool.get(selectedIndex);
      setSlot(
          47,
          ItemBuilder.of(Material.GOLD_NUGGET)
              .customName("<gold>选中: " + sel.templateId())
              .lore(List.of("<gray>骨币 " + sel.priceBones() + " · 权重 " + sel.weight()))
              .build(),
          (p, e) -> {});
    }

    setSlot(
        49,
        ItemBuilder.of(Material.ARROW)
            .customName("<gray>返回")
            .lore(List.of("<gray>回到商店配置"))
            .build(),
        (p, e) -> new ShopAdminMenu(p.player()).open());

    MenuPagination.placeCornerArrows(
        this::setSlot, currentPage, pool.size(), nextPage -> new ShopAdminPoolMenu(player.player(), nextPage).open());
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
    return "刷新池" + MenuPagination.titleSuffix(page, pool().size());
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.admin";
  }

  /** 从卡牌库分页挑选并加入刷新池。 */
  public static final class ShopAdminPoolAddMenu extends Menu {

    private final ShopAdminPoolMenu parent;
    private final int page;

    ShopAdminPoolAddMenu(Player player, ShopAdminPoolMenu parent, int page) {
      super(player);
      this.parent = parent;
      this.page = Math.max(0, page);
    }

    @Override
    protected void setup() {
      inventory.clear();
      List<CardTemplate> templates = CardCatalog.sortedForEditor();
      int currentPage = MenuPagination.clampPage(page, templates.size());
      int start = MenuPagination.rangeStart(currentPage);
      int end = MenuPagination.rangeEnd(currentPage, templates.size());

      int contentSlot = 0;
      for (int i = start; i < end; i++) {
        CardTemplate t = templates.get(i);
        setSlot(
            contentSlot++,
            ItemBuilder.of(t.spawnEggMaterial())
                .customName("<green>" + t.displayName())
                .lore(
                    List.of(
                        "<gray>ID: <white>" + t.id(),
                        "<gray>力/命: <white>" + t.power() + "/" + t.health(),
                        "<yellow>点击加入刷新池"))
                .build(),
            (p, e) -> {
              parent.addFromHand(t.id());
              parent.open();
            });
      }

      setSlot(
          49,
          ItemBuilder.of(Material.ARROW)
              .customName("<gray>返回")
              .lore(List.of("<gray>回到刷新池编辑"))
              .build(),
          (p, e) -> parent.open());

      MenuPagination.placeCornerArrows(
          (s, item, action) -> setSlot(s, item, action),
          currentPage,
          templates.size(),
          nextPage -> new ShopAdminPoolAddMenu(player.player(), parent, nextPage).open());
    }

    @Override
    protected String getTitle() {
      return "选择卡牌" + MenuPagination.titleSuffix(page, CardCatalog.sortedForEditor().size());
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
}
