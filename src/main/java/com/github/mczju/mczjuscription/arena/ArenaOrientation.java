package com.github.mczju.mczjuscription.arena;

/** 11×11 场地朝向：row0（敌人后场 / 双人对方 UI）所在边。 */
public enum ArenaOrientation {
    /** row0 在 Z 较小一侧，列索引向 +X 增加（默认）。 */
    SOUTH,
    /** row0 在 Z 较大一侧。 */
    NORTH,
    /** row0 在 X 较小一侧，列索引向 +Z 增加。 */
    EAST,
    /** row0 在 X 较大一侧。 */
    WEST;

    public static ArenaOrientation parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return SOUTH;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return SOUTH;
        }
    }
}
