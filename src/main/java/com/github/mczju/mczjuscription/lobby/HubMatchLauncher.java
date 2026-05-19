package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.player.party.Party;
import org.jetbrains.annotations.Nullable;

/** 从大厅座位 / 指令开局（优先插件内分配 play 房间，不修改 GameCore）。 */
public final class HubMatchLauncher {

    private HubMatchLauncher() {}

    public static void startSolo(PlayerExt player, PlayVariant variant) {
        InscriptionHubMatchStarter.startSolo(player, variant);
    }

    public static void startSolo(PlayerExt player, PlayVariant variant, @Nullable String roomId) {
        InscriptionHubMatchStarter.startSolo(player, variant, roomId);
    }

    public static void startDuel(Party party, PlayVariant variant) {
        InscriptionHubMatchStarter.startDuel(party, variant);
    }

    public static void startDuel(Party party, PlayVariant variant, @Nullable String roomId) {
        InscriptionHubMatchStarter.startDuel(party, variant, roomId);
    }

    /** 解析 {@code /isc start solo shop} 等形式。 */
    public static PlayVariant parseVariant(String modeArg, String deckArg) {
        if (modeArg == null || deckArg == null) {
            return null;
        }
        String mode = modeArg.toLowerCase();
        String deck = deckArg.toLowerCase();
        boolean solo = mode.equals("solo") || mode.equals("single") || mode.equals("单人");
        boolean duel = mode.equals("duel") || mode.equals("pvp") || mode.equals("双人");
        boolean shop = deck.equals("shop") || deck.equals("商店");
        boolean free = deck.equals("free") || deck.equals("freebuild") || deck.equals("构牌") || deck.equals("自由");
        if (solo && shop) {
            return PlayVariant.SOLO_SHOP;
        }
        if (solo && free) {
            return PlayVariant.SOLO_FREE_BUILD;
        }
        if (duel && shop) {
            return PlayVariant.DUEL_SHOP;
        }
        if (duel && free) {
            return PlayVariant.DUEL_FREE_BUILD;
        }
        return null;
    }
}
