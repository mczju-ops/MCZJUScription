package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.deck.DefaultDeckLists;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 自由构建模式：编辑 {@link InscriptionPlayerData#savedDeck} */
public class DeckBuilderMenu extends Menu {

  private static final int DECK_INFO_SLOT = 45;
  private static final int DESIGNER_SLOT = 47;
  private static final int SAVE_SLOT = 49;
  private static final int CLEAR_SLOT = 50;

  private final List<String> editing;
  private final int page;

  /** GameCore {@link com.github.mczjuops.mczjugamecore.menu.MenuFacade} 要求此签名（Object[]，非可变参数）。 */
  public DeckBuilderMenu(Player player, Object[] args) {
    this(player, null, 0);
  }

  DeckBuilderMenu(Player player, List<String> editing, int page) {
    super(player);
    this.page = Math.max(0, page);
    if (editing != null) {
      this.editing = editing;
    } else {
      InscriptionPlayerData data =
          new com.github.mczjuops.mczjugamecore.player.PlayerExt(player)
              .getData(InscriptionPlayerData.class);
      List<String> loaded = DefaultDeckLists.parseDeck(data.savedDeck);
      this.editing =
          new ArrayList<>(loaded.isEmpty() ? DefaultDeckLists.starterFreeBuildDeck() : loaded);
    }
  }

  @Override
  protected void setup() {
    inventory.clear();

    List<String> pool = CardCatalog.deckBuilderPool();
    int currentPage = MenuPagination.clampPage(page, pool.size());
    int start = MenuPagination.rangeStart(currentPage);
    int end = MenuPagination.rangeEnd(currentPage, pool.size());

    int contentSlot = 0;
    for (int i = start; i < end; i++) {
      String id = pool.get(i);
      setSlot(
          contentSlot++,
          ItemBuilder.of(CardCatalog.require(id).spawnEggMaterial())
              .customName("<green>+ " + CardCatalog.require(id).displayName())
              .lore(List.of("<gray>点击加入牌组"))
              .build(),
          (p, e) -> {
            editing.add(id);
            refresh();
          });
    }

    setSlot(
        DECK_INFO_SLOT,
        ItemBuilder.of(Material.WRITABLE_BOOK)
            .customName("<aqua>当前牌组 <gray>(" + editing.size() + " 张)")
            .lore(List.of("<yellow>左键打开查看", "<gray>相同卡牌会堆叠显示", "<gray>查看页按 Q 移除一张"))
            .build(),
        (p, e) -> {
          if (e.isLeftClick()) {
            new DeckViewMenu(p.player(), editing, currentPage).open();
          }
        });

    setSlot(
        DESIGNER_SLOT,
        ItemBuilder.of(Material.ANVIL)
            .customName("<light_purple>卡牌设计器")
            .lore(List.of("<gray>制作 / 编辑卡牌模板"))
            .build(),
        (p, e) -> new CardDesignerMenu(p.player(), new Object[0]).open());

    setSlot(
        SAVE_SLOT,
        ItemBuilder.of(Material.EMERALD)
            .customName("<green>保存牌组")
            .lore(List.of("<gray>写入玩家数据，构牌模式生效"))
            .glint(true)
            .build(),
        (p, e) -> save());

    setSlot(
        CLEAR_SLOT,
        ItemBuilder.of(Material.BARRIER)
            .customName("<red>清空牌组")
            .build(),
        (p, e) ->
            new AlertMenu(
                    p.player(),
                    () -> new DeckBuilderMenu(p.player(), new ArrayList<>(editing), currentPage)
                        .open())
                .open());

    MenuPagination.placeCornerArrows(
        this::setSlot,
        currentPage,
        pool.size(),
        nextPage ->
            new DeckBuilderMenu(player.player(), new ArrayList<>(editing), nextPage).open());
  }

  private void save() {
    InscriptionPlayerData data = player.getData(InscriptionPlayerData.class);
    data.savedDeck = new ArrayList<>(editing);
    data.setModified(true);
    player.sender().success("<green>牌组已保存（共 %d 张）".formatted(editing.size()));
    player.player().closeInventory();
  }

  @Override
  protected String getTitle() {
    return "编辑牌组" + MenuPagination.titleSuffix(page, CardCatalog.deckBuilderPool().size());
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
