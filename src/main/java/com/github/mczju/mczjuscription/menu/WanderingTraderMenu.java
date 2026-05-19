package com.github.mczju.mczjuscription.menu;

import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.roguelike.TraderCosts;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 流浪商人主菜单。 */
public final class WanderingTraderMenu extends Menu {

  private final InscriptionMatch match;
  private final MatchSide side;

  public WanderingTraderMenu(Player player, InscriptionMatch match, MatchSide side) {
    super(player, match, side);
    this.match = match;
    this.side = side;
  }

  @Override
  protected void setup() {
    inventory.clear();
    setSlot(
        11,
        ItemBuilder.of(Material.ENCHANTED_BOOK)
            .customName("<light_purple>印制印记")
            .lore(
                List.of(
                    "<gray>为手牌中的卡添加 1 个印记",
                    "<dark_gray>每张卡最多 3 个",
                    "<red>花费：腐肉 ×" + TraderCosts.IMPRINT_BLOOD))
            .build(),
        (p, e) -> new TraderImprintMenu(p.player(), match, side).open());

    setSlot(
        15,
        ItemBuilder.of(Material.ANVIL)
            .customName("<gold>融合两张卡")
            .lore(
                List.of(
                    "<gray>属性、印记、费用相加",
                    "<gray>名称拼接，骑乘模型",
                    "<red>花费：腐肉 ×" + TraderCosts.FUSION_BLOOD))
            .build(),
        (p, e) -> new TraderFusionMenu(p.player(), match, side).open());

    setSlot(
        22,
        ItemBuilder.of(Material.BARRIER)
            .customName("<red>离开")
            .build(),
        (p, e) -> p.player().closeInventory());
  }

  @Override
  protected String getTitle() {
    return "流浪商人";
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 3;
  }

  @Override
  protected String getPermission() {
    return "inscription.play";
  }
}
