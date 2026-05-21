package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.SigilRules;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilDescriptions;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.roguelike.TraderCosts;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 流浪商人：为手牌随机印制一个印记。 */
public final class TraderImprintMenu extends Menu {

  private final InscriptionMatch match;
  private final MatchSide side;
  private String selectedCardId;

  public TraderImprintMenu(Player player, InscriptionMatch match, MatchSide side) {
    super(player, match, side);
    this.match = match;
    this.side = side;
  }

  @Override
  protected void setup() {
    inventory.clear();
    List<String> hand = match.participant(side).hand();
    int slot = 0;
    for (String cardId : hand) {
      if (slot >= 18) break;
      CardTemplate t = CardCatalog.require(cardId);
      boolean selected = cardId.equals(selectedCardId);
      setSlot(
          slot++,
          ItemBuilder.of(t.spawnEggMaterial())
              .customName((selected ? "<green>✓ " : "") + "<white>" + t.displayName())
              .lore(
                  List.of(
                      "<dark_purple>印记: " + t.sigilsDisplay(),
                      "<gray>点击选择手牌"))
              .build(),
          (p, e) -> {
            selectedCardId = cardId;
            setup();
          });
    }

    if (selectedCardId != null) {
      setSlot(
          22,
          ItemBuilder.of(Material.ENCHANTED_BOOK)
              .customName("<gold>随机印制印记")
              .lore(
                  List.of(
                      "<gray>从印记池随机抽取 1 个",
                      "<red>花费：腐肉 ×" + TraderCosts.IMPRINT_BLOOD,
                      "<yellow>点击确认"))
              .build(),
          (p, e) -> applyRandomSigil(p.player()));
    }

    setSlot(
        26,
        ItemBuilder.of(Material.ARROW).customName("<gray>返回").build(),
        (p, e) -> new WanderingTraderMenu(p.player(), match, side).open());
  }

  private void applyRandomSigil(Player p) {
    if (selectedCardId == null) {
      p.sendMessage("§c请先选择一张手牌。");
      return;
    }
    CardTemplate base = CardCatalog.require(selectedCardId);
    Set<SigilId> current = SigilRules.asSet(base.sigils());
    List<SigilId> pool = new ArrayList<>();
    for (SigilId sigil : SigilDescriptions.traderImprintPool()) {
      if (!current.contains(sigil) && SigilRules.canAdd(current, sigil)) {
        pool.add(sigil);
      }
    }
    if (pool.isEmpty()) {
      p.sendMessage("§c没有可印制的印记（已满或池为空）。");
      return;
    }
    SigilId rolled = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    if (!TraderCosts.trySpendBlood(match, side, p, TraderCosts.IMPRINT_BLOOD)) {
      return;
    }
    List<SigilId> next = new ArrayList<>(base.sigils());
    next.add(rolled);
    String newId = CardCatalog.newCustomId();
    CardTemplate upgraded = base.copy(newId);
    upgraded.setSigils(next);
    CardCatalog.saveRuntime(upgraded);

    var hand = match.participant(side).hand();
    int idx = hand.indexOf(selectedCardId);
    if (idx >= 0) {
      hand.set(idx, newId);
    }
    InscriptionItems.syncHandItems(p, hand);
    match.syncHud();
    p.sendMessage(
        "§a已为 §f"
            + upgraded.displayName()
            + " §a随机印制「"
            + SigilNames.display(rolled)
            + "」。");
    selectedCardId = newId;
    setup();
  }

  @Override
  protected String getTitle() {
    return "印制印记";
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 4;
  }

  @Override
  protected String getPermission() {
    return "inscription.play";
  }
}
