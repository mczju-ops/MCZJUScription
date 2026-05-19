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

    public static final float PEDAL_WIDTH = 0.8f;
    public static final float PEDAL_HALF_WIDTH = PEDAL_WIDTH / 2f;

    /** 相对房间标点的 Y 抬高（踏板厚度中心对齐用，与 XZ 角点偏移无关）。 */
    public static final double SLOT_Y_OFFSET = 0.5;

    private ArenaCoordinates() {}

    /** 房间标点 = 槽位/钟的逻辑中心（落牌判定、造物站位等）。 */
    public static Location toSlotCenter(Location configured) {
        if (configured == null) return null;
        return configured.clone().add(0, SLOT_Y_OFFSET, 0);
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

    /** @deprecated 使用 {@link #toSlotCenter} 或 {@link #toPedalSpawn} */
    @Deprecated
    public static Location toSlotLocation(Location configured) {
        return toPedalSpawn(configured);
    }
}
