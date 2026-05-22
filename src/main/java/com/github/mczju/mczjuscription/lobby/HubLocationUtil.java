package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;

/** 大厅交互点检测。 */
public final class HubLocationUtil {

    private static final double INTERACT_RADIUS = 1.75;

    private HubLocationUtil() {}

    public static boolean sameBlock(Location configured, Location at) {
        if (configured == null || at == null) return false;
        World cw = configured.getWorld();
        World aw = at.getWorld();
        if (cw == null || aw == null || !cw.getUID().equals(aw.getUID())) {
            return false;
        }
        return configured.getBlockX() == at.getBlockX()
                && configured.getBlockY() == at.getBlockY()
                && configured.getBlockZ() == at.getBlockZ();
    }

    public static boolean near(Location configured, Location at) {
        if (configured == null || at == null) return false;
        World cw = configured.getWorld();
        World aw = at.getWorld();
        if (cw == null || aw == null || !cw.getUID().equals(aw.getUID())) {
            return false;
        }
        return configured.distanceSquared(at) <= INTERACT_RADIUS * INTERACT_RADIUS;
    }

    /** 配置坐标所在方块的几何中心（x/z/y 均为 block + 0.5）。 */
    public static Location blockCenter(Location configured) {
        if (configured == null || configured.getWorld() == null) {
            return null;
        }
        return new Location(
                configured.getWorld(),
                configured.getBlockX() + 0.5,
                configured.getBlockY() + 0.5,
                configured.getBlockZ() + 0.5);
    }

    /** 座位展示实体生成点（0.8 立方体对齐方块中心，见 {@link HubSeatMarkerService}）。 */
    public static Location seatMarkerSpawn(Location configured, float scale) {
        Location center = blockCenter(configured);
        if (center == null) {
            return null;
        }
        float half = scale / 2f;
        return center.subtract(half, half, half);
    }

    public static List<Location> seatInteractionPoints(InscriptionGameRoom room) {
        List<Location> list = new ArrayList<>(3);
        Location solo = soloSeat(room);
        if (solo != null) {
            list.add(solo);
        }
        list.addAll(duelSeats(room));
        return list;
    }

    public static Location soloSeat(InscriptionGameRoom room) {
        if (room.soloSeat != null) {
            return room.soloSeat;
        }
        return room.soloSeat0;
    }

    public static List<Location> duelSeats(InscriptionGameRoom room) {
        List<Location> list = new ArrayList<>(2);
        if (room.duelSeat0 != null) {
            list.add(room.duelSeat0);
        }
        if (room.duelSeat1 != null) {
            list.add(room.duelSeat1);
        }
        return list;
    }

    /** 点击的是第几个双人座（0 或 1），未命中返回 -1。 */
    public static int duelSeatIndex(InscriptionGameRoom room, Location click) {
        if (near(room.duelSeat0, click)) {
            return 0;
        }
        if (near(room.duelSeat1, click)) {
            return 1;
        }
        return -1;
    }
}
