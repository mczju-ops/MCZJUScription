package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.data.CardDesignerSession;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.SigilRules;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.ui.DialogTextInput;
import com.github.mczju.mczjuscription.util.SpawnEggEntityTypes;
import org.bukkit.inventory.ItemStack;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 卡牌制作：生物蛋模型、0~3 印记、力量/生命、骨币/腐肉召唤代价。 */
public final class CardDesignerMenu extends Menu {

  public static final int MODEL_SLOT = 4;

  private final CardDesignerSession session;

  public CardDesignerMenu(Player player, Object[] args) {
    super(player, args);
    this.session = CardDesignerSession.of(player.getUniqueId());
    if (session.editingId() == null) {
      session.newCard(CardCatalog.newCustomId());
    }
  }

  public CardDesignerSession session() {
    return session;
  }

  @Override
  protected void setup() {
    inventory.clear();
    CardTemplate preview = session.toTemplate(false);

    setSlot(
        MODEL_SLOT,
        ItemBuilder.of(preview.spawnEggMaterial())
            .customName("<aqua>模型: <white>" + preview.displayName())
            .lore(
                List.of(
                    "<gray>当前: <white>"
                        + SpawnEggEntityTypes.displayEntity(session.entityType()),
                    "<yellow>将怪物蛋放入此格",
                    "<gray>（从背包点选或拖入，不消耗）",
                    "<dark_purple>印记: <light_purple>" + session.sigilSummary()))
            .build(),
        (p, e) ->
            p.player()
                .sendMessage("§e请从背包拿取怪物蛋，点击或放入上方模型格以选定生物。"));

    setSlot(
        22,
        ItemBuilder.of(Material.WRITABLE_BOOK)
            .customName("<dark_purple>印记")
            .lore(
                List.of(
                    "<gray>已选: <white>"
                        + session.sigils().size()
                        + "/"
                        + SigilRules.MAX_PER_CARD,
                    "<gray>当前: <light_purple>" + session.sigilSummary(),
                    "<yellow>点击打开印记列表"))
            .build(),
        (p, e) -> new CardDesignerSigilMenu(p.player(), this, session, 0).open());

    setSlot(
        11,
        ItemBuilder.of(Material.IRON_SWORD)
            .customName("<gold>力量: <white>" + session.power())
            .lore(List.of("<gray>左键 +1  <gray>右键 -1"))
            .build(),
        (p, e) -> {
          session.addPower(e.isLeftClick() ? 1 : -1);
          reloadEditor();
        });

    setSlot(
        15,
        ItemBuilder.of(Material.APPLE)
            .customName("<red>生命: <white>" + session.health())
            .lore(List.of("<gray>左键 +1  <gray>右键 -1"))
            .build(),
        (p, e) -> {
          session.addHealth(e.isLeftClick() ? 1 : -1);
          reloadEditor();
        });

    setSlot(
        37,
        ItemBuilder.of(Material.BONE)
            .customName("<gold>骨币代价: <white>" + session.boneCost())
            .lore(List.of("<gray>左键 +1  <gray>右键 -1", "<dark_gray>保存为骨币召唤费"))
            .build(),
        (p, e) -> {
          session.addBoneCost(e.isLeftClick() ? 1 : -1);
          reloadEditor();
        });

    setSlot(
        38,
        ItemBuilder.of(Material.COD)
            .customName("<aqua>鱼干代价: <white>" + session.fishCost())
            .lore(List.of("<gray>左键 +1  <gray>右键 -1", "<dark_gray>保存为鱼干召唤费"))
            .build(),
        (p, e) -> {
          session.addFishCost(e.isLeftClick() ? 1 : -1);
          reloadEditor();
        });

    setSlot(
        39,
        ItemBuilder.of(Material.ROTTEN_FLESH)
            .customName("<red>腐肉代价: <white>" + session.bloodCost())
            .lore(List.of("<gray>左键 +1  <gray>右键 -1", "<dark_gray>保存为腐肉召唤费"))
            .build(),
        (p, e) -> {
          session.addBloodCost(e.isLeftClick() ? 1 : -1);
          reloadEditor();
        });

    setSlot(
        40,
        ItemBuilder.of(Material.NAME_TAG)
            .customName("<aqua>名称: <white>" + session.displayName())
            .lore(List.of("<yellow>点击打开对话框输入名称", "<dark_gray>最多 32 字"))
            .build(),
        (p, e) -> DialogTextInput.openCardName(p.player(), session.displayName()));

    String editingId = session.editingId();
    boolean deletable = editingId != null && CardCatalog.canDelete(editingId);
    setSlot(
        47,
        ItemBuilder.of(deletable ? Material.RED_CONCRETE : Material.BARRIER)
            .customName(deletable ? "<red>删除卡牌" : "<dark_gray>删除卡牌")
            .lore(
                deletable
                    ? List.of(
                        "<gray>ID: <white>" + editingId,
                        "<yellow>从 cards.yml 移除",
                        "<red>不可撤销，请确认")
                    : List.of("<dark_gray>卡牌不存在"))
            .build(),
        (p, e) -> {
          if (editingId == null) {
            return;
          }
          if (!CardCatalog.canDelete(editingId)) {
            p.player().sendMessage("§c卡牌不存在或已删除。");
            return;
          }
          CardTemplate existing = CardCatalog.get(editingId);
          String label =
              existing != null ? existing.displayName() + " (" + editingId + ")" : editingId;
          new AlertMenu(
                  p.player(),
                  () -> {
                    CardCatalog.DeleteResult result = CardCatalog.deleteCard(editingId);
                    switch (result) {
                      case REMOVED ->
                          p.player()
                              .sendMessage("§a已删除卡牌 §f" + label + "§a。");
                      case NOT_FOUND -> p.player().sendMessage("§c卡牌不存在或已删除。");
                    }
                    session.newCard(CardCatalog.newCustomId());
                    new CardDesignerMenu(p.player(), new Object[0]).open();
                  })
              .open();
        });

    setSlot(
        49,
        ItemBuilder.of(Material.LIME_CONCRETE)
            .customName("<green>保存卡牌")
            .lore(List.of("<gray>写入 cards.yml 并热加载"))
            .build(),
        (p, e) -> {
          if (session.boneCost() > 0
              && (session.bloodCost() > 0 || session.fishCost() > 0)) {
            p.player()
                .sendMessage(
                    "§e多种代价同时大于 0，保存时将优先使用 §f骨币 §e（"
                        + session.boneCost()
                        + "）。");
          } else if (session.fishCost() > 0 && session.bloodCost() > 0) {
            p.player()
                .sendMessage(
                    "§e鱼干与腐肉代价同时大于 0，保存时将优先使用 §f鱼干 §e（"
                        + session.fishCost()
                        + "）。");
          }
          CardTemplate saved = session.toTemplate(false);
          CardCatalog.save(saved);
          p.player()
              .sendMessage(
                  "§a已保存卡牌 §f" + saved.id() + " §a（" + saved.displayName() + "）");
        });

    setSlot(
        48,
        ItemBuilder.of(Material.CHEST)
            .customName("<yellow>加载已有卡牌")
            .lore(List.of("<gray>打开卡牌列表"))
            .build(),
        (p, e) -> new CardPickerMenu(p.player(), this, 0).open());

    setSlot(
        50,
        ItemBuilder.of(Material.PAPER)
            .customName("<aqua>新建空白卡")
            .build(),
        (p, e) -> {
          session.newCard(CardCatalog.newCustomId());
          reloadEditor();
        });

    setSlot(
        51,
        ItemBuilder.of(Material.CHEST_MINECART)
            .customName("<green>获取到手上")
            .lore(
                List.of(
                    "<gray>按当前界面数值发放 1 张",
                    "<dark_gray>无需先保存（会热加载预览）",
                    "<gray>ID: <white>" + (editingId != null ? editingId : "—")))
            .build(),
        (p, e) -> givePreviewToHand(p.player()));
  }

  private void givePreviewToHand(Player player) {
    String id = session.editingId();
    if (id == null || id.isBlank()) {
      player.sendMessage("§c无有效卡牌 ID，无法发放。");
      return;
    }
    CardTemplate preview = session.toTemplate(false);
    CardCatalog.saveRuntime(preview);
    ItemStack card = InscriptionItems.card(id).getItem();
    var leftover = player.getInventory().addItem(card);
    if (!leftover.isEmpty()) {
      player.sendMessage("§e背包已满，多余卡牌已掉落在脚边。");
      leftover.values()
          .forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }
    player.sendMessage(
        "§a已获得卡牌 §f%s §a（%s）。".formatted(preview.displayName(), id));
  }

  public void reloadEditor() {
    setup();
  }

  private CardDesignerSession sessionForTitle() {
    if (session != null) {
      return session;
    }
    return CardDesignerSession.of(player.player().getUniqueId());
  }

  @Override
  protected String getTitle() {
    String id = sessionForTitle().editingId();
    return "卡牌设计 · " + (id != null ? id : "新卡");
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 6;
  }

  @Override
  protected String getPermission() {
    return "inscription.admin";
  }

  /** 选择要编辑的模板。 */
  public static final class CardPickerMenu extends Menu {

    private final CardDesignerMenu parent;
    private final int page;

    CardPickerMenu(Player player, CardDesignerMenu parent, int page) {
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

      int slot = 0;
      for (int i = start; i < end; i++) {
        CardTemplate t = templates.get(i);
        setSlot(
            slot++,
            ItemBuilder.of(t.spawnEggMaterial())
                .customName("<green>" + t.displayName())
                .lore(
                    List.of(
                        "<gray>ID: <white>" + t.id(),
                        "<gray>力/命: <white>" + t.power() + "/" + t.health(),
                        "<dark_purple>印记: <light_purple>" + t.sigilsDisplay()))
                .build(),
            (p, e) -> {
              CardDesignerSession.of(p.player().getUniqueId()).load(t);
              parent.open();
            });
      }

      setSlot(
          49,
          ItemBuilder.of(Material.ARROW)
              .customName("<gray>返回")
              .lore(List.of("<gray>回到卡牌设计器"))
              .build(),
          (p, e) -> parent.open());

      MenuPagination.placeCornerArrows(
          this::setSlot,
          currentPage,
          templates.size(),
          nextPage -> new CardPickerMenu(player.player(), parent, nextPage).open());
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
