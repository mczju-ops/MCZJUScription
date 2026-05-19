package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.card.CardRegistry;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.shop.ShopCatalog;
import com.github.mczju.mczjuscription.shop.ShopPresenter;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

import java.util.List;

public final class InscriptionShopMenu extends Menu {

    private final InscriptionMatch match;
    private final MatchSide buyerSide;
    private final ShopPresenter presenter;

    public InscriptionShopMenu(Player player, InscriptionMatch match, MatchSide buyerSide, ShopPresenter presenter) {
        // Menu 构造器在子类字段赋值前会调用 getTitle()，须把 match 放进 args
        super(player, match, buyerSide, presenter);
        this.match = match;
        this.buyerSide = buyerSide;
        this.presenter = presenter;
    }

    @Override
    protected void setup() {
        inventory.clear();
        int bones = match.currency(buyerSide).getBones();
        CardId[] cards = ShopCatalog.availableCards();
        for (int i = 0; i < cards.length && i < getRows() * 9 - 9; i++) {
            CardId id = cards[i];
            int price = ShopCatalog.bonePrice(id);
            boolean affordable = price == 0 || bones >= price;
            int slot = i;
            setSlot(
                    slot,
                    ItemBuilder.of(CardRegistry.get(id).spawnEggMaterial())
                            .customName("<gold>" + CardRegistry.get(id).displayName())
                            .lore(List.of(
                                    "<gray>价格: <gold>" + price + " 骨币",
                                    affordable ? "<green>点击购买" : "<red>骨币不足"
                            ))
                            .build(),
                    (p, event) -> {
                        if (!affordable) {
                            match.feedback().actionBarWarn("<red>骨币不足 ×%d".formatted(price));
                            return;
                        }
                        if (presenter.purchase(match, buyerSide, id)) {
                            p.player().closeInventory();
                        }
                    }
            );
        }
        setSlot(
                22,
                ItemBuilder.of(Material.BARRIER)
                        .customName("<red>关闭")
                        .lore(List.of("<gray>未购牌前无法进入出牌阶段", "<gray>可先购买免费兔子（商店）或抽兔子堆"))
                        .build(),
                (p, event) -> p.player().closeInventory()
        );
    }

    @Override
    protected String getTitle() {
        InscriptionMatch m = match;
        MatchSide side = buyerSide;
        if (m == null && args.length >= 1 && args[0] instanceof InscriptionMatch found) {
            m = found;
        }
        if (side == null && args.length >= 2 && args[1] instanceof MatchSide found) {
            side = found;
        }
        if (m == null || side == null) {
            return "卡牌商店";
        }
        return "卡牌商店 (骨币: " + m.currency(side).getBones() + ")";
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
