package com.github.mczju.mczjuscription.lobby.menu;

import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczju.mczjuscription.lobby.HubMatchLauncher;
import com.github.mczju.mczjuscription.lobby.HubSession;
import com.github.mczjuops.mczjugamecore.menu.AlertMenu;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 双人座位：选模式并确认诅咒后开始。 */
public final class DuelModePickMenu extends Menu {

    private PlayVariant selected;

    public DuelModePickMenu(Player player) {
        super(player);
    }

    @Override
    protected void setup() {
        inventory.clear();

        setSlot(
                10,
                ItemBuilder.of(Material.EMERALD)
                        .customName("<gold>双人 · 商店")
                        .lore(List.of("<gray>2 人队伍", "<yellow>点击选择"))
                        .glint(selected == PlayVariant.DUEL_SHOP)
                        .build(),
                (p, e) -> {
                    selected = PlayVariant.DUEL_SHOP;
                    setup();
                });

        setSlot(
                14,
                ItemBuilder.of(Material.WRITABLE_BOOK)
                        .customName("<aqua>双人 · 构牌")
                        .lore(List.of("<gray>2 人队伍", "<yellow>点击选择"))
                        .glint(selected == PlayVariant.DUEL_FREE_BUILD)
                        .build(),
                (p, e) -> {
                    selected = PlayVariant.DUEL_FREE_BUILD;
                    setup();
                });

        InscriptionPlayerData data =
                player.getData(AbstractInscriptionGame.DATA_ID, InscriptionPlayerData.class);
        String curse = data.selectedCurse == null ? "none" : data.selectedCurse;

        setSlot(
                16,
                ItemBuilder.of(Material.WITHER_SKELETON_SKULL)
                        .customName("<dark_red>当前诅咒")
                        .lore(
                                List.of(
                                        "<gray>" + curseDisplay(curse),
                                        "<gray>在诅咒区可修改"))
                        .build(),
                (p, e) -> {});

        setSlot(
                22,
                ItemBuilder.of(Material.LIME_CONCRETE)
                        .customName("<green>确认并开始")
                        .lore(List.of("<gray>需队长确认队伍诅咒"))
                        .build(),
                (p, e) -> confirmStart(p));

        setSlot(
                26,
                ItemBuilder.of(Material.BARRIER).customName("<red>取消").build(),
                (p, e) -> p.player().closeInventory());
    }

    private void confirmStart(PlayerExt ext) {
        if (selected == null) {
            ext.sender().warn("请先选择模式。");
            return;
        }
        if (!ext.isPartyLeader()) {
            ext.sender().warn("仅队长可以开始双人游戏。");
            return;
        }
        if (ext.getParty() == null || ext.getParty().getAllPlayer().size() != 2) {
            ext.sender().warn("需要 2 人队伍。");
            return;
        }
        InscriptionPlayerData data =
                ext.getData(AbstractInscriptionGame.DATA_ID, InscriptionPlayerData.class);
        String curse = data.selectedCurse == null ? "none" : data.selectedCurse;
        String curseLine = curseDisplay(curse);

        HubSession.setPendingDuel(ext.getUniqueId(), selected);
        ext.player().closeInventory();
        ext.sender().info("<yellow>请确认开局 · 诅咒：<white>%s".formatted(curseLine));
        new AlertMenu(
                        ext.player(),
                        () -> {
                            HubSession.markDuelConfirmed(ext.getUniqueId());
                            HubMatchLauncher.startDuel(ext.getParty(), selected);
                        })
                .open();
    }

    private static String curseDisplay(String id) {
        return switch (id.toLowerCase()) {
            case "glass" -> "玻璃之魂（占位）";
            case "ring" -> "戒环（占位）";
            case "none" -> "无";
            default -> id;
        };
    }

    @Override
    protected String getTitle() {
        return "双人 · 选择模式";
    }

    @Override
    protected @Range(from = 1, to = 6) int getRows() {
        return 3;
    }

    @Override
    protected String getPermission() {
        return "inscription.hub";
    }
}
