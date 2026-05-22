package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.data.CardDesignerSession;
import com.github.mczju.mczjuscription.game.card.SigilRules;
import com.github.mczju.mczjuscription.game.sigil.SigilDescriptions;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 卡牌设计器：勾选印记（最多 3），保存后写回会话。 */
public final class CardDesignerSigilMenu extends Menu {

  private final CardDesignerMenu parent;
  private final CardDesignerSession session;
  private final Set<SigilId> pending;
  private final int page;

  CardDesignerSigilMenu(Player player, CardDesignerMenu parent, CardDesignerSession session, int page) {
    this(player, parent, session, page, null);
  }

  CardDesignerSigilMenu(
      Player player,
      CardDesignerMenu parent,
      CardDesignerSession session,
      int page,
      Set<SigilId> initialPending) {
    super(player);
    this.parent = parent;
    this.session = session;
    this.page = Math.max(0, page);
    this.pending = EnumSet.noneOf(SigilId.class);
    if (initialPending != null) {
      this.pending.addAll(initialPending);
    } else {
      this.pending.addAll(session.sigils());
    }
  }

  @Override
  protected void setup() {
    inventory.clear();
    List<SigilId> sigils = SigilDescriptions.implementedForDesigner();
    int currentPage = MenuPagination.clampPage(page, sigils.size());
    int start = MenuPagination.rangeStart(currentPage);
    int end = MenuPagination.rangeEnd(currentPage, sigils.size());

    int slot = 0;
    for (int i = start; i < end; i++) {
      SigilId sigil = sigils.get(i);
      boolean on = pending.contains(sigil);
      boolean implemented = SigilDescriptions.isImplemented(sigil);
      List<String> lore = new ArrayList<>(SigilDescriptions.loreLines(sigil));
      if (!implemented) {
        lore.add("<red>战斗逻辑尚未接入");
      }
      setSlot(
          slot++,
          ItemBuilder.of(
                  !implemented
                      ? Material.GRAY_DYE
                      : (on ? Material.ENCHANTED_BOOK : Material.BOOK))
              .customName(
                  (on ? "<green>✓ " : "<gray>")
                      + SigilNames.display(sigil)
                      + (implemented ? "" : " <dark_gray>(未接)"))
              .lore(lore)
              .build(),
          (p, e) -> toggle(sigil));
    }

    setSlot(
        49,
        ItemBuilder.of(Material.ARROW)
            .customName("<gray>返回")
            .lore(List.of("<gray>不保存更改"))
            .build(),
        (p, e) -> parent.open());

    setSlot(
        51,
        ItemBuilder.of(Material.LIME_CONCRETE)
            .customName("<green>保存印记")
            .lore(
                List.of(
                    "<gray>已选: <white>" + pending.size() + "/" + SigilRules.MAX_PER_CARD,
                    "<gray>写回卡牌并返回设计器"))
            .build(),
        (p, e) -> {
          session.setSigils(new ArrayList<>(pending));
          parent.reloadEditor();
          parent.open();
        });

    MenuPagination.placeCornerArrows(
        this::setSlot,
        currentPage,
        sigils.size(),
        nextPage ->
            new CardDesignerSigilMenu(player.player(), parent, session, nextPage, pending).open());
  }

  private void toggle(SigilId sigil) {
    if (pending.contains(sigil)) {
      pending.remove(sigil);
    } else if (pending.size() >= SigilRules.MAX_PER_CARD) {
      player.sender().warn("<red>印记已满（最多 %d 个）".formatted(SigilRules.MAX_PER_CARD));
      return;
    } else {
      pending.add(sigil);
    }
    setup();
  }

  @Override
  protected String getTitle() {
    return "选择印记"
        + MenuPagination.titleSuffix(page, SigilDescriptions.implementedForDesigner().size());
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
