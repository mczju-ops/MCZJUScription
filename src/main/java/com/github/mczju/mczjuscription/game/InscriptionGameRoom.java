package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.arena.ArenaLayoutResolver;
import com.github.mczju.mczjuscription.arena.ResolvedArenaLayout;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczjuops.mczjugamecore.game.room.JsonGameRoom;
import org.bukkit.Location;

/**
 * 邪恶冥刻房间配置（大厅 + 对局场地，{@code plugins/MCZJUGameCore/rooms/inscription/*.json}）。
 * <p>
 * 对局场地：11×17 统一矩形 {@link #arenaCornerA} / {@link #arenaCornerB} + {@link #arenaOrientation}。
 */
public class InscriptionGameRoom extends JsonGameRoom {

    /** 大厅出生点（hub 房间使用）。 */
    public Location spawnAt;

    public Integer playerCandles;
    public Integer enemyCandles;

    /** 11×17 场地矩形角点 A。 */
    public Location arenaCornerA;
    /** 11×17 场地矩形角点 B。 */
    public Location arenaCornerB;
    /** SOUTH / NORTH / EAST / WEST：row0（敌后场或双人对方 UI）所在边。 */
    public String arenaOrientation;

    /**
     * UI 图标绕 X 轴旋转偏移（度，叠加在 -90° 躺平基准上）。
     * 可用 {@code /isc arena ui icon pitch} 局内微调后写入 JSON。
     */
    public Float arenaUiIconPitchDeg;

    /**
     * UI 图标绕 Y 轴旋转偏移（度，叠加在朝向默认角上）。
     * 可用 {@code /isc arena ui icon yaw} 局内微调后写入 JSON。
     */
    public Float arenaUiIconYawDeg;

    /**
     * UI 图标绕 Z 轴旋转偏移（度，平面内滚转）。
     * 可用 {@code /isc arena ui icon roll} 局内微调后写入 JSON。
     */
    public Float arenaUiIconRollDeg;

    public Location deckLecternAt;
    public Location blessingAt;
    public Location curseSelectAt;
    /** 通关榜 TextDisplay 位置（MGC 同步）；右键附近可手动刷新。 */
    public Location leaderboardAt;

    public Location soloSeat;
    public Location duelSeat0;
    public Location duelSeat1;

    @Deprecated
    public Location soloSeat0;

    public boolean hasConfiguredArena(MatchMode mode) {
        return hasUnifiedArena();
    }

    public boolean hasUnifiedArena() {
        return arenaCornerA != null && arenaCornerB != null;
    }

    public com.github.mczju.mczjuscription.arena.ArenaOrientation arenaOrientation() {
        return com.github.mczju.mczjuscription.arena.ArenaOrientation.parse(arenaOrientation);
    }

    public ResolvedArenaLayout resolveLayout(MatchMode mode) {
        return ArenaLayoutResolver.resolve(this, mode);
    }

    public Location spawnFor(com.github.mczju.mczjuscription.game.match.MatchSide side, MatchMode mode) {
        ResolvedArenaLayout.StagingSites sites = resolveLayout(mode).staging(side);
        if (sites != null && sites.playerSpawn != null) {
            return sites.playerSpawn.clone();
        }
        return null;
    }
}
