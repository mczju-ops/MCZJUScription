package com.github.mczju.mczjuscription.lobby;

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

    public static List<Location> soloSeats(InscriptionHubRoom room) {
        return collect(room.soloSeat0, room.soloSeat1, room.soloSeat2, room.soloSeat3);
    }

    public static List<Location> duelSeats(InscriptionHubRoom room) {
        return collect(room.duelSeat0, room.duelSeat1, room.duelSeat2, room.duelSeat3);
    }

    public static List<HubSign> signs(InscriptionHubRoom room) {
        List<HubSign> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Location at = signAt(room, i);
            String text = signText(room, i);
            if (at != null && text != null && !text.isBlank()) {
                list.add(new HubSign(i, at, text));
            }
        }
        return list;
    }

    private static Location signAt(InscriptionHubRoom room, int i) {
        return switch (i) {
            case 0 -> room.signAt0;
            case 1 -> room.signAt1;
            case 2 -> room.signAt2;
            case 3 -> room.signAt3;
            case 4 -> room.signAt4;
            case 5 -> room.signAt5;
            case 6 -> room.signAt6;
            case 7 -> room.signAt7;
            default -> null;
        };
    }

    private static String signText(InscriptionHubRoom room, int i) {
        return switch (i) {
            case 0 -> room.signText0;
            case 1 -> room.signText1;
            case 2 -> room.signText2;
            case 3 -> room.signText3;
            case 4 -> room.signText4;
            case 5 -> room.signText5;
            case 6 -> room.signText6;
            case 7 -> room.signText7;
            default -> null;
        };
    }

    @SafeVarargs
    private static List<Location> collect(Location... locs) {
        List<Location> out = new ArrayList<>();
        for (Location loc : locs) {
            if (loc != null) {
                out.add(loc);
            }
        }
        return out;
    }

    public record HubSign(int index, Location at, String text) {}
}
