package com.github.mczju.mczjuscription.arena;

import org.bukkit.Location;

/**
 * 房间配置坐标 → 对局坐标。
 * <p>
 * BlockDisplay 以方块「角点」为局部原点 (0,0,0)，缩放后向 +X/+Z 延伸。
 * 踏板 XZ 缩放到 {@link #PEDAL_WIDTH} 时，几何中心在角点 +{@link #PEDAL_HALF_WIDTH}，
 * 故展示实体应生成在 {@code 配置中心 + (-PEDAL_HALF_WIDTH, …, -PEDAL_HALF_WIDTH)}。
 */
public final class ArenaCoordinates {

    public static final float PEDAL_WIDTH = 1.8f;
    public static final float PEDAL_HALF_WIDTH = PEDAL_WIDTH / 2f;

    /** 相对房间标点的 Y 抬高（踏板厚度中心对齐用，与 XZ 角点偏移无关）。 */
    public static final double SLOT_Y_OFFSET = 0.5;

    private ArenaCoordinates() {}

    /** 房间标点 = 槽位/钟的逻辑中心（落牌判定、造物站位等）。 */
    public static Location toSlotCenter(Location configured) {
        if (configured == null) return null;
        return configured.clone().add(0, SLOT_Y_OFFSET, 0);
    }

    /** 房间标点（方块 X/Z 中心，Y 为方块底面）→ 方块角点。 */
    public static Location toBlockCorner(Location configured) {
        if (configured == null || configured.getWorld() == null) {
            return null;
        }
        return configured.getBlock().getLocation();
    }

    /** 方块角点 → 几何中心（+0.5）。 */
    public static Location cornerToBlockCenter(Location blockCorner) {
        if (blockCorner == null) {
            return null;
        }
        return blockCorner.clone().add(0.5, 0.5, 0.5);
    }

    /** 房间标点 → 方块几何中心。 */
    public static Location toBlockCenter(Location configured) {
        Location corner = toBlockCorner(configured);
        return corner == null ? null : cornerToBlockCenter(corner);
    }

    /**
     * BlockDisplay 生成点（局部角点）。
     *
     * @param xzHalfSize 展示物 XZ 缩放尺寸的一半（踏板 0.4，钟 0.3 等）
     */
    public static Location toDisplaySpawn(Location configured, double xzHalfSize) {
        if (configured == null) return null;
        return configured.clone().add(-xzHalfSize, SLOT_Y_OFFSET, -xzHalfSize);
    }

    public static Location toPedalSpawn(Location configured) {
        return toDisplaySpawn(configured, PEDAL_HALF_WIDTH);
    }

    /** 长条踏板（11×2 区域）生成点；长边沿棋盘宽度方向。 */
    public static Location toStripPedalSpawn(Location stripCenter, ArenaOrientation orientation) {
        return toStripPedalSpawn(stripCenter, orientation, ArenaGridParser.STRIP_PEDAL_LENGTH);
    }

    /** 内嵌 1.8×10.8 条带踏板，居中于 2×11 区域。 */
    public static Location toInnerStripPedalSpawn(Location stripCenter, ArenaOrientation orientation) {
        return toStripPedalSpawn(stripCenter, orientation, ArenaGridParser.INNER_STRIP_PEDAL_LENGTH);
    }

    /** @deprecated 使用 {@link #toInnerStripPedalSpawn} */
    @Deprecated
    public static Location toPreviewInnerPedalSpawn(Location stripCenter, ArenaOrientation orientation) {
        return toInnerStripPedalSpawn(stripCenter, orientation);
    }

    private static Location toStripPedalSpawn(
            Location stripCenter, ArenaOrientation orientation, float pedalLength) {
        if (stripCenter == null) {
            return null;
        }
        float halfLen = pedalLength / 2f;
        float halfDepth = ArenaGridParser.STRIP_PEDAL_DEPTH / 2f;
        if (stripLengthAlongWorldZ(orientation)) {
            return stripCenter.clone().add(-halfDepth, SLOT_Y_OFFSET, -halfLen);
        }
        return stripCenter.clone().add(-halfLen, SLOT_Y_OFFSET, -halfDepth);
    }

    /** EAST/WEST 时条带长边沿世界 Z，否则沿世界 X。 */
    public static boolean stripLengthAlongWorldZ(ArenaOrientation orientation) {
        return orientation == ArenaOrientation.EAST || orientation == ArenaOrientation.WEST;
    }

    /** @deprecated 使用 {@link #toStripPedalSpawn(Location, ArenaOrientation)} */
    @Deprecated
    public static Location toStripPedalSpawn(Location stripCenter) {
        return toStripPedalSpawn(stripCenter, ArenaOrientation.SOUTH);
    }

    public static Location toStripCenter(Location configured) {
        return configured == null ? null : configured.clone().add(0, SLOT_Y_OFFSET, 0);
    }

    /** @deprecated 使用 {@link #toSlotCenter} 或 {@link #toPedalSpawn} */
    @Deprecated
    public static Location toSlotLocation(Location configured) {
        return toPedalSpawn(configured);
    }
}
