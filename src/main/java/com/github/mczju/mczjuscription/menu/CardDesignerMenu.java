package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.data.CardDesignerSession;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.SigilRules;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 卡牌制作：生物蛋模型、0~3 印记、力量/生命/召唤代价。 */
public final class CardDesignerMenu extends Menu {

  private final CardDesignerSession session;

  public CardDesignerMenu(Player player, Object[] args) {
    super(player, args);
    this.session = CardDesignerSession.of(player.getUniqueId());
    if (session.editingId() == null) {
      session.newCard(CardCatalog.newCustomId());
    }
  }

  @Override
  protected void setup() {
    inventory.clear();
    CardTemplate preview = session.toTemplate(false);

    setSlot(
        4,
        ItemBuilder.of(preview.spawnEggMaterial())
            .customName("<aqua>模型: <white>" + preview.displayName())
            .lore(
                List.of(
                    "<gray>当前实体: <white>" + session.entityType().name(),
                    "<yellow>点击切换生物蛋",
                    "<gray>印记 %d/%d"
                        .formatted(session.sigils().size(), SigilRules.MAX_PER_CARD)))
            .build(),
        (p, e) -> {
          session.cycleEntity();
          reloadEditor();
        });

    SigilId[] pick =
        new SigilId[] {
          SigilId.RABBIT_HOLE,
          SigilId.AIR_STRIKE,
          SigilId.FLEDGLING,
          SigilId.BREEDING,
          SigilId.TOUCH_OF_DEATH,
          SigilId.STINKY,
          SigilId.GUARD_DOG,
          SigilId.TUTOR,
          SigilId.SPLIT_STRIKE
        };
    int[] sigilSlots = {19, 20, 21, 23, 24, 25, 28, 29, 30};
    for (int i = 0; i < pick.length && i < sigilSlots.length; i++) {
      SigilId sigil = pick[i];
      boolean on = session.sigils().contains(sigil);
      setSlot(
          sigilSlots[i],
          ItemBuilder.of(on ? Material.ENCHANTED_BOOK : Material.BOOK)
              .customName((on ? "<green>✓ " : "<gray>") + SigilNames.display(sigil))
              .lore(
                  List.of(
                      on ? "<gray>点击移除" : "<gray>点击添加",
                      "<dark_gray>每张卡最多 " + SigilRules.MAX_PER_CARD + " 个印记"))
              .build(),
          (p, e) -> {
            if (!session.toggleSigil(sigil)) {
              p.player().sendMessage("§c印记已满（最多 %d 个）".formatted(SigilRules.MAX_PER_CARD));
            }
            reloadEditor();
          });
    }

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
        38,
        ItemBuilder.of(Material.GOLD_NUGGET)
            .customName("<gold>召唤代价")
            .lore(
                List.of(
                    "<gray>类型: <white>" + session.costType().name(),
                    "<gray>数值: <white>" + session.cost(),
                    "<yellow>点击切换类型",
                    "<gray>Shift+左键 +1  Shift+右键 -1"))
            .build(),
        (p, e) -> {
          if (e.isShiftClick()) {
            session.addCost(e.isLeftClick() ? 1 : -1);
          } else {
            session.cycleCostType();
          }
          reloadEditor();
        });

    setSlot(
        40,
        ItemBuilder.of(Material.NAME_TAG)
            .customName("<aqua>名称: <white>" + session.displayName())
            .lore(List.of("<gray>在聊天栏输入: <white>/isc cardname <名称>"))
            .build(),
        (p, e) ->
            p.player()
                .sendMessage("§e请使用 §f/isc cardname <名称>§e 修改卡牌名称"));

    setSlot(
        49,
        ItemBuilder.of(Material.LIME_CONCRETE)
            .customName("<green>保存卡牌")
            .lore(List.of("<gray>写入 cards.yml 并热加载"))
            .build(),
        (p, e) -> {
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
        (p, e) -> new CardPickerMenu(p.player(), this).open());

    setSlot(
        50,
        ItemBuilder.of(Material.PAPER)
            .customName("<aqua>新建空白卡")
            .build(),
        (p, e) -> {
          session.newCard(CardCatalog.newCustomId());
          reloadEditor();
        });
  }

  void reloadEditor() {
    setup();
  }

  @Override
  protected String getTitle() {
    return "卡牌设计 · " + (session.editingId() != null ? session.editingId() : "新卡");
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

    CardPickerMenu(Player player, CardDesignerMenu parent) {
      super(player);
      this.parent = parent;
    }

    @Override
    protected void setup() {
      inventory.clear();
      List<CardTemplate> templates = new ArrayList<>(CardCatalog.sortedForEditor());
      int slot = 0;
      for (CardTemplate t : templates) {
        if (slot >= getRows() * 9 - 9) break;
        setSlot(
            slot++,
            ItemBuilder.of(t.spawnEggMaterial())
                .customName((t.isBuiltin() ? "<gold>" : "<green>") + t.displayName())
                .lore(
                    List.of(
                        "<gray>ID: <white>" + t.id(),
                        "<gray>力/命: <white>" + t.power() + "/" + t.health(),
                        "<dark_purple>印记: <light_purple>" + t.sigilsDisplay()))
                .build(),
            (p, e) -> {
              CardDesignerSession.of(p.player().getUniqueId()).load(t);
              parent.reloadEditor();
              p.player().closeInventory();
            });
      }
      setSlot(
          getRows() * 9 - 5,
          ItemBuilder.of(Material.ARROW).customName("<gray>返回").build(),
          (p, e) -> parent.open());
    }

    @Override
    protected String getTitle() {
      return "选择卡牌模板";
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
