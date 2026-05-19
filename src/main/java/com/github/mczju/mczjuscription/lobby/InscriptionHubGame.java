package com.github.mczju.mczjuscription.lobby;

import com.github.mczjuops.mczjugamecore.game.GameMeta;
import com.github.mczjuops.mczjugamecore.game.impl.OpenSessionGame;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.util.List;
import org.bukkit.Material;

/** 邪恶冥刻共享等待大厅（OpenSession，不创建对局）。 */
public final class InscriptionHubGame extends OpenSessionGame {

    public static final String GAME_ID = "inscription_hub";

    @Override
    public String getId() {
        return GAME_ID;
    }

    @Override
    protected boolean onGameInit() {
        InscriptionHubRoom room = hubRoom();
        if (room != null) {
            HubSignageService.ensureSpawned(room);
        }
        return true;
    }

    @Override
    public GameMeta getGameMeta() {
        return GameMeta.builder()
                .displayName("<dark_red>邪恶冥刻")
                .icon(Material.CANDLE)
                .author("<green>MCZJU")
                .description(
                        List.of(
                                "<gray>等待大厅 · 构牌 / 选座开局",
                                "<gray>单人座：商店或自由构牌",
                                "<gray>双人座：组队后选模式",
                                "<yellow>▶ 点击进入大厅"))
                .build();
    }

    @Override
    public void onPlayerJoin(PlayerExt player) {
        player.switchProfile(GAME_ID);
        player.resetState();
        InscriptionHubRoom room = hubRoom();
        if (room != null) {
            HubSignageService.ensureSpawned(room);
            if (room.spawnAt != null) {
                player.player().teleport(room.spawnAt);
            }
        } else {
            player.sender().warn("大厅房间未配置，请管理员创建 inscription_hub 房间。");
        }
        HubSeatService.release(player.getUniqueId());
        player.sender()
                .info(
                        "<gold>邪恶冥刻大厅</gold>：讲台构牌、赐福/诅咒区、排行榜、单人/双人座位。");
    }

    @Override
    public void onPlayerQuit(PlayerExt player) {
        HubSeatService.dismount(player.player());
        HubSeatService.release(player.getUniqueId());
        player.switchProfile(null);
    }

    public InscriptionHubRoom hubRoom() {
        if (getGameRoom() instanceof InscriptionHubRoom hub) {
            return hub;
        }
        return null;
    }
}
