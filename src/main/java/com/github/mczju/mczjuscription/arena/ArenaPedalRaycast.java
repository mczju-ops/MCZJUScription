package com.github.mczju.mczjuscription.arena;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * 数学射线与踏板水平 AABB 求交（隔空选中，不依赖实体碰撞箱）。
 * <p>
 * 踏板为地面薄板，使用「视线与踏板高度平面求交 + XZ 容差」，比薄 3D 盒更稳定。
 */
public final class ArenaPedalRaycast {

    /** 最大瞄准距离（格）。 */
    public static final double MAX_DISTANCE = 48.0;
    /** XZ 方向在踏板半宽上的额外容差（格）。 */
    private static final double HIT_PAD = 0.35;

    private ArenaPedalRaycast() {}

    public static ArenaPedalTarget trace(BattleArena arena, Location eye, Vector direction) {
        if (arena == null || eye == null || direction == null || eye.getWorld() == null) {
            return null;
        }
        Vector dir = direction.clone();
        if (dir.lengthSquared() < 1e-8) {
            return null;
        }
        dir.normalize();

        ArenaPedalTarget best = null;
        double bestT = Double.MAX_VALUE;
        for (BattleArena.PedalHitVolume volume : arena.pedalHitVolumes()) {
            Double t = intersectPedalPlane(eye, dir, volume, MAX_DISTANCE);
            if (t == null || t >= bestT) {
                continue;
            }
            bestT = t;
            best = volume.target();
        }
        return best;
    }

    /** 视线与踏板高度平面求交，命中 XZ 扩展盒则有效。 */
    private static Double intersectPedalPlane(
            Location eye, Vector dir, BattleArena.PedalHitVolume volume, double maxDistance) {
        double planeY = volume.center().getY();
        if (Math.abs(dir.getY()) < 1e-6) {
            return null;
        }
        double t = (planeY - eye.getY()) / dir.getY();
        if (t < 0.0 || t > maxDistance) {
            return null;
        }
        double px = eye.getX() + dir.getX() * t;
        double pz = eye.getZ() + dir.getZ() * t;
        if (!insideExpandedXZ(px, pz, volume)) {
            return null;
        }
        return t;
    }

    private static boolean insideExpandedXZ(double px, double pz, BattleArena.PedalHitVolume volume) {
        double cx = volume.center().getX();
        double cz = volume.center().getZ();
        return Math.abs(px - cx) <= volume.halfX() + HIT_PAD
                && Math.abs(pz - cz) <= volume.halfZ() + HIT_PAD;
    }
}
