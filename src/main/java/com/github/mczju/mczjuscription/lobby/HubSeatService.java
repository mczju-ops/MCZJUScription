package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** 大厅座位：占座、传送、椅子骑乘。 */
public final class HubSeatService {

    private static final String SOLO_SEAT_KEY = "solo";
    private static final String DUEL_PAIR_KEY = "duel";

    private HubSeatService() {}

    public static boolean occupySolo(PlayerExt player, InscriptionGameRoom room) {
        Location seat = HubLocationUtil.soloSeat(room);
        if (seat == null) {
            player.sender().warn("未配置单人座位 soloSeat。");
            return false;
        }
        if (player.isInParty()) {
            player.sender().warn("请先 <white>/party leave</white> 再使用单人座位。");
            return false;
        }
        if (!HubSession.tryOccupy(player.getUniqueId(), SOLO_SEAT_KEY, HubSession.SeatKind.SOLO)) {
            player.sender().warn("单人座位已被占用。");
            return false;
        }
        sitAt(player, seat);
        return true;
    }

    /**
     * 双人座：队长在任一席位右键开启；两名队员分别坐到 {@code duelSeat0}、{@code duelSeat1}。
     */
    public static boolean occupyDuel(PlayerExt leader, InscriptionGameRoom room) {
        List<Location> seats = HubLocationUtil.duelSeats(room);
        if (seats.size() < 2) {
            leader.sender().warn("未配置两个双人座位 duelSeat0、duelSeat1。");
            return false;
        }
        if (!leader.isInParty()) {
            leader.sender().warn("双人模式需要 2 人队伍，请先用 <white>/party invite</white> 组队。");
            return false;
        }
        if (leader.getParty().getAllPlayer().size() != 2) {
            leader.sender().warn("双人模式需要恰好 2 人的队伍。");
            return false;
        }
        if (!leader.isPartyLeader()) {
            leader.sender().warn("请让队长在双人座位处开启。");
            return false;
        }
        if (!HubSession.tryOccupy(leader.getUniqueId(), DUEL_PAIR_KEY, HubSession.SeatKind.DUEL)) {
            leader.sender().warn("双人座位已被占用。");
            return false;
        }

        List<PlayerExt> members = leader.getParty().getAllPlayer();
        Location seatA = seats.get(0);
        Location seatB = seats.get(1);
        for (int i = 0; i < members.size(); i++) {
            PlayerExt member = members.get(i);
            HubSession.tryOccupy(member.getUniqueId(), DUEL_PAIR_KEY, HubSession.SeatKind.DUEL);
            sitAt(member, i == 0 ? seatA : seatB);
        }
        return true;
    }

    public static void release(UUID playerId) {
        UUID mountId = HubSession.mountOf(playerId);
        if (mountId != null) {
            Entity mount = findEntity(mountId);
            if (mount != null) {
                mount.remove();
            }
        }
        HubSession.release(playerId);
    }

    public static void dismount(Player player) {
        if (player.getVehicle() != null) {
            player.leaveVehicle();
        }
    }

    private static void sitAt(PlayerExt ext, Location seat) {
        Player player = ext.player();
        Location dest = seat.clone();
        player.teleport(dest);
        spawnChair(player, dest);
    }

    private static void spawnChair(Player player, Location at) {
        dismount(player);
        ArmorStand stand =
                at.getWorld()
                        .spawn(
                                at.clone().add(0, -0.45, 0),
                                ArmorStand.class,
                                entity -> {
                                    entity.setVisible(false);
                                    entity.setGravity(false);
                                    entity.setInvulnerable(true);
                                    entity.setMarker(true);
                                    entity.setSmall(true);
                                });
        stand.addPassenger(player);
        HubSession.bindMount(player.getUniqueId(), stand);
    }

    private static Entity findEntity(UUID id) {
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
