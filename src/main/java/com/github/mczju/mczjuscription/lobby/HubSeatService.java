package com.github.mczju.mczjuscription.lobby;

import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/** 大厅座位：占座、传送、椅子骑乘。 */
public final class HubSeatService {

    private HubSeatService() {}

    public static boolean occupySolo(PlayerExt player, InscriptionHubRoom room, int seatIndex) {
        List<Location> seats = HubLocationUtil.soloSeats(room);
        if (seatIndex < 0 || seatIndex >= seats.size()) {
            return false;
        }
        if (player.isInParty()) {
            player.sender().warn("请先 <white>/party leave</white> 再使用单人座位。");
            return false;
        }
        String key = "solo:" + seatIndex;
        if (!HubSession.tryOccupy(player.getUniqueId(), key, HubSession.SeatKind.SOLO)) {
            player.sender().warn("该座位已被占用。");
            return false;
        }
        sitAt(player, seats.get(seatIndex));
        return true;
    }

    public static boolean occupyDuel(PlayerExt player, InscriptionHubRoom room, int seatIndex) {
        List<Location> seats = HubLocationUtil.duelSeats(room);
        if (seatIndex < 0 || seatIndex >= seats.size()) {
            return false;
        }
        if (!player.isInParty()) {
            player.sender().warn("双人模式需要 2 人队伍，请先用 <white>/party invite</white> 组队。");
            return false;
        }
        if (player.getParty().getAllPlayer().size() != 2) {
            player.sender().warn("双人模式需要恰好 2 人的队伍。");
            return false;
        }
        if (!player.isPartyLeader()) {
            player.sender().warn("请让队长选择双人座位。");
            return false;
        }
        String key = "duel:" + seatIndex;
        if (!HubSession.tryOccupy(player.getUniqueId(), key, HubSession.SeatKind.DUEL)) {
            player.sender().warn("该双人座位已被占用。");
            return false;
        }
        for (PlayerExt member : player.getParty().getAllPlayer()) {
            HubSession.tryOccupy(member.getUniqueId(), key, HubSession.SeatKind.DUEL);
            sitAt(member, seats.get(seatIndex));
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
