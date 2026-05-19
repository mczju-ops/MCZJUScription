package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardFusion;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.roguelike.TraderCosts;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 流浪商人：融合手牌中两张卡。 */
public final class TraderFusionMenu extends Menu {

  private final InscriptionMatch match;
  private final MatchSide side;
  private String firstId;
  private String secondId;

  public TraderFusionMenu(Player player, InscriptionMatch match, MatchSide side) {
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
      if (slot >= 22) break;
      CardTemplate t = CardCatalog.require(cardId);
      String mark = "";
      if (cardId.equals(firstId)) mark = "<green>[A] ";
      if (cardId.equals(secondId)) mark = "<gold>[B] ";
      setSlot(
          slot++,
          ItemBuilder.of(t.spawnEggMaterial())
              .customName(mark + "<white>" + t.displayName())
              .lore(
                  List.of(
                      "<gray>力/命 " + t.power() + "/" + t.health(),
                      "<dark_purple>" + t.sigilsDisplay(),
                      "<gray>左键选 A · 右键选 B"))
              .build(),
          (p, e) -> {
            if (e.isLeftClick()) {
              firstId = cardId;
            } else {
              secondId = cardId;
            }
            setup();
          });
    }

    setSlot(
        25,
        ItemBuilder.of(Material.ANVIL)
            .customName("<green>确认融合")
            .lore(
                List.of(
                    "<gray>需要已选 A 与 B 两张不同卡",
                    "<red>花费：腐肉 ×" + TraderCosts.FUSION_BLOOD))
            .build(),
        (p, e) -> confirmFusion(p.player()));

    setSlot(
        26,
        ItemBuilder.of(Material.ARROW).customName("<gray>返回").build(),
        (p, e) -> new WanderingTraderMenu(p.player(), match, side).open());
  }

  private void confirmFusion(Player p) {
    if (firstId == null || secondId == null || firstId.equals(secondId)) {
      p.sendMessage("§c请选择两张不同的手牌。");
      return;
    }
    var hand = match.participant(side).hand();
    if (!hand.contains(firstId) || !hand.contains(secondId)) {
      p.sendMessage("§c手牌已变化，请重新选择。");
      return;
    }
    if (!TraderCosts.trySpendBlood(match, side, p, TraderCosts.FUSION_BLOOD)) {
      return;
    }
    CardTemplate a = CardCatalog.require(firstId);
    CardTemplate b = CardCatalog.require(secondId);
    CardTemplate fused = CardFusion.fuse(a, b);
    hand.remove(firstId);
    hand.remove(secondId);
    hand.add(fused.id());
    InscriptionItems.syncHandItems(p, hand);
    match.syncHud();
    p.sendMessage("§a融合成功：§f" + fused.displayName());
    firstId = null;
    secondId = null;
    p.closeInventory();
  }

  @Override
  protected String getTitle() {
    return "融合卡牌";
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
