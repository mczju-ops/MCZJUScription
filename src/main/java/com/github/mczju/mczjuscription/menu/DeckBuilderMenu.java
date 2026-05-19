package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.deck.DefaultDeckLists;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;

/** 自由构建模式：编辑 {@link InscriptionPlayerData#savedDeck} */
public class DeckBuilderMenu extends Menu {

    private final List<String> editing;

    /** GameCore {@link com.github.mczjuops.mczjugamecore.menu.MenuFacade} 要求此签名（Object[]，非可变参数）。 */
    public DeckBuilderMenu(Player player, Object[] args) {
        super(player, args);
        InscriptionPlayerData data = new com.github.mczjuops.mczjugamecore.player.PlayerExt(player)
                .getData(AbstractInscriptionGame.DATA_ID, InscriptionPlayerData.class);
        List<String> loaded = DefaultDeckLists.parseDeck(data.savedDeck);
        this.editing = new ArrayList<>(loaded.isEmpty() ? DefaultDeckLists.starterFreeBuildDeck() : loaded);
    }

    @Override
    protected void setup() {
        inventory.clear();

        setSlot(4, ItemBuilder.of(Material.WRITABLE_BOOK)
                .customName("<aqua>当前牌组 <gray>(" + editing.size() + " 张)")
                .lore(buildDeckLore())
                .build());

        CardId[] pool = {
            CardId.RABBIT, CardId.HARE, CardId.BEE, CardId.FROG, CardId.BAT,
            CardId.WOLF_CUB, CardId.WOLF, CardId.MANTIS, CardId.GRIZZLY, CardId.GOAT,
            CardId.GUARDIAN, CardId.MOLE, CardId.GECKO, CardId.PACK_RAT, CardId.GHOUL,
            CardId.AMALGAM, CardId.CHICKEN, CardId.BONE_LORD
        };
        for (int i = 0; i < pool.length; i++) {
            CardId id = pool[i];
            int slot = 9 + i;
            if (slot >= getRows() * 9) break;
            setSlot(slot, ItemBuilder.of(CardCatalog.require(id.name()).spawnEggMaterial())
                    .customName("<green>+ " + CardCatalog.require(id.name()).displayName())
                    .lore(List.of("<gray>点击加入牌组"))
                    .build(), (p, e) -> {
                editing.add(id.name());
                refresh();
            });
        }

        setSlot(
            8,
            ItemBuilder.of(Material.ANVIL)
                .customName("<light_purple>卡牌设计器")
                .lore(List.of("<gray>制作 / 编辑卡牌模板"))
                .build(),
            (p, e) -> new CardDesignerMenu(p.player(), new Object[0]).open());

        setSlot(getRows() * 9 - 5, ItemBuilder.of(Material.EMERALD)
                .customName("<green>保存牌组")
                .lore(List.of("<gray>写入玩家数据，构牌模式生效"))
                .glint(true)
                .build(), (p, e) -> save());

        setSlot(getRows() * 9 - 1, ItemBuilder.of(Material.BARRIER)
                .customName("<red>清空牌组")
                .build(), (p, e) -> new AlertMenu(p.player(), () -> {
            editing.clear();
            refresh();
        }).open());
    }

    private void save() {
        InscriptionPlayerData data = player.getData(AbstractInscriptionGame.DATA_ID, InscriptionPlayerData.class);
        data.savedDeck = new java.util.ArrayList<>(editing);
        data.setModified(true);
        player.sender().success("<green>牌组已保存（共 %d 张）".formatted(editing.size()));
        player.player().closeInventory();
    }

    private List<String> buildDeckLore() {
        List<String> lore = new ArrayList<>();
        for (String id : editing) {
            lore.add("<gray>• " + CardCatalog.require(id).displayName());
        }
        if (lore.isEmpty()) lore.add("<dark_gray>（空）");
        return lore;
    }

    @Override
    protected String getTitle() {
        return "编辑牌组";
    }

    @Override
    protected @Range(from = 1, to = 6) int getRows() {
        return 3;
    }

    @Override
    protected String getPermission() {
        return "inscription.deck";
    }
}
