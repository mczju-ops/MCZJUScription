package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.data.CardDesignerSession;
import com.github.mczju.mczjuscription.menu.CardDesignerMenu;
import com.github.mczju.mczjuscription.menu.ShopAdminPoolMenu;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import com.github.mczju.mczjuscription.shop.ShopPoolEntry;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.utils.DialogBuilder;
import org.bukkit.entity.Player;

/** 使用 GameCore {@link DialogBuilder} 的文本/数字输入。 */
public final class DialogTextInput {

  private static final int CARD_NAME_MAX_LEN = 32;

  private DialogTextInput() {}

  public static void openShopWeight(Player player, int poolIndex, int currentWeight) {
    PlayerExt ext = new PlayerExt(player);
    player.closeInventory();
    String hint = "<gray>当前权重：<white>%d".formatted(currentWeight);
    DialogBuilder.of("<yellow>设置刷新池权重")
        .emptyLine()
        .textInput("value", hint)
        .showConfirm(
            player,
            150,
            "确认",
            (p, r) -> {
              String input = r.text("value") != null ? r.text("value").trim() : "";
              try {
                int weight = Integer.parseInt(input);
                if (weight < 1 || weight > 999_999) {
                  throw new NumberFormatException();
                }
                applyShopWeight(p, ext, poolIndex, weight);
              } catch (NumberFormatException e) {
                ext.sender()
                    .error(
                        "<red>输入格式错误：\"%s\" 不是 1～999999 的整数".formatted(input));
                openShopWeight(p, poolIndex, currentWeight);
              }
            },
            "取消",
            (p, r) -> reopenShopPool(p, poolIndex));
  }

  public static void openCardName(Player player, String currentName) {
    PlayerExt ext = new PlayerExt(player);
    player.closeInventory();
    String safeCurrent = currentName != null ? currentName : "";
    String hint =
        safeCurrent.isEmpty()
            ? "<yellow>请输入卡牌名称"
            : "<gray>当前名称：<white>%s".formatted(safeCurrent);
    DialogBuilder.of("<yellow>设置卡牌名称")
        .emptyLine()
        .textInput("value", hint, CARD_NAME_MAX_LEN, 200, safeCurrent)
        .showConfirm(
            player,
            150,
            "确认",
            (p, r) -> {
              String input = r.text("value") != null ? r.text("value").trim() : "";
              if (input.isEmpty() || input.length() > CARD_NAME_MAX_LEN) {
                ext.sender()
                    .error(
                        "<red>名称不能为空，且不超过 %d 个字符".formatted(CARD_NAME_MAX_LEN));
                openCardName(p, safeCurrent);
                return;
              }
              CardDesignerSession.of(p.getUniqueId()).setDisplayName(input);
              ext.sender().success("<green>卡牌名称已设为：<white>%s".formatted(input));
              new CardDesignerMenu(p, new Object[0]).open();
            },
            "取消",
            (p, r) -> new CardDesignerMenu(p, new Object[0]).open());
  }

  private static void applyShopWeight(Player player, PlayerExt ext, int poolIndex, int weight) {
    var pool = ShopConfigStorage.get().rotatingPool();
    if (poolIndex < 0 || poolIndex >= pool.size()) {
      ext.sender().error("<red>条目已不存在，请重新打开刷新池。");
      new ShopAdminPoolMenu(player).open();
      return;
    }
    ShopPoolEntry cur = pool.get(poolIndex);
    pool.set(poolIndex, new ShopPoolEntry(cur.templateId(), cur.priceBones(), weight));
    ext.sender()
        .success(
            "<green>已设置 <dark_aqua>%s</dark_aqua> 权重为 <dark_green>%d"
                .formatted(cur.templateId(), weight));
    reopenShopPool(player, poolIndex);
  }

  private static void reopenShopPool(Player player, int poolIndex) {
    ShopAdminPoolMenu menu = new ShopAdminPoolMenu(player);
    if (poolIndex >= 0) {
      menu.setSelectedIndex(poolIndex);
    }
    menu.open();
  }
}
