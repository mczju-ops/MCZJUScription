package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.shop.ShopConfig;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 管理员：商店全局配置入口。 */
public final class ShopAdminMenu extends Menu {

  public ShopAdminMenu(Player player) {
    super(player);
  }

  @Override
  protected void setup() {
    inventory.clear();
    ShopConfig cfg = ShopConfigStorage.get();

    setSlot(
        4,
        ItemBuilder.of(Material.CHEST)
            .customName("<gold>商店配置")
            .lore(
                List.of(
                    "<gray>常驻 4 格（每回合总共购 1 张）",
                    "<gray>刷新 3+1 格（第 4 格腐肉解锁）",
                    "<gray>配置写入 shop.yml"))
            .build(),
        (p, e) -> {});

    setSlot(
        10,
        ItemBuilder.of(Material.ANVIL)
            .customName("<aqua>常驻货架 ×4")
            .lore(List.of("<yellow>点击编辑", "<gray>手持卡牌放入槽位"))
            .build(),
        (p, e) -> new ShopAdminPermanentMenu(p.player()).open());

    setSlot(
        19,
        ItemBuilder.of(Material.HOPPER)
            .customName("<yellow>刷新池")
            .lore(
                List.of(
                    "<gray>当前条目: <white>" + cfg.rotatingPool().size(),
                    "<gray>每回合随机抽 4 张（第 4 格需解锁才显示）",
                    "<yellow>点击编辑价格与权重"))
            .build(),
        (p, e) -> new ShopAdminPoolMenu(p.player()).open());

    setSlot(
        22,
        ItemBuilder.of(Material.ROTTEN_FLESH)
            .customName("<red>第 4 刷新格解锁腐肉")
            .lore(
                List.of(
                    "<gray>当前: <white>" + cfg.extraSlotUnlockBlood(),
                    "<gray>左键 +1  右键 -1",
                    "<dark_gray>玩家本局花腐肉解锁第 4 格"))
            .build(),
        (p, e) -> {
          int delta = e.isLeftClick() ? 1 : -1;
          cfg.setExtraSlotUnlockBlood(cfg.extraSlotUnlockBlood() + delta);
          setup();
        });

    setSlot(
        49,
        ItemBuilder.of(Material.LIME_CONCRETE)
            .customName("<green>保存配置")
            .lore(List.of("<gray>写入 plugins/.../shop.yml"))
            .build(),
        (p, e) -> {
          ShopConfigStorage.save();
          p.player().sendMessage("§a商店配置已保存。");
        });
  }

  @Override
  protected String getTitle() {
    return "商店配置";
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
