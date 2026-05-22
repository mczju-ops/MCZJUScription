package com.github.mczju.mczjuscription.arena;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * 从框选矩形角点解析 11×17 场地：
 * <ul>
 *   <li>顶 2×11：预览条（单人）或对方敲钟条（双人）</li>
 *   <li>中 11×11：4×4 战斗/UI 矩阵（槽间 1 格）</li>
 *   <li>底 2×11：己方敲钟条</li>
 * </ul>
 * 各条带之间各留 1 格间隔。
 */
public final class ArenaGridParser {

    public static final int ARENA_COLS = 11;
    public static final int ARENA_ROWS = 17;
    public static final int STRIP_ROWS = 2;
    public static final int SECTION_GAP = 1;
    public static final int MAIN_GRID_ROWS = 11;
    /** 主网格在全场矩形中的起始行（跳过顶条 + 间隔）。 */
    public static final int MAIN_GRID_START = STRIP_ROWS + SECTION_GAP;
    /** 底敲钟条起始行。 */
    public static final int BELL_STRIP_START = MAIN_GRID_START + MAIN_GRID_ROWS + SECTION_GAP;

    public static final int SLOT_STRIDE = 3;
    public static final int SLOT_COUNT_PER_ROW = 4;

    /** 2×11 条带踏板（敲钟条等）：深 1.8 × 长 19.8。 */
    public static final float STRIP_PEDAL_LENGTH = 19.8f;
    public static final float STRIP_PEDAL_DEPTH = 1.8f;
    /** 内嵌条带踏板（预览/敲钟）：深 1.8 × 长 10.8，居中于 2×11 区域。 */
    public static final float INNER_STRIP_PEDAL_LENGTH = 10.8f;

    /** @deprecated 使用 {@link #INNER_STRIP_PEDAL_LENGTH} */
    @Deprecated
    public static final float PREVIEW_INNER_PEDAL_LENGTH = INNER_STRIP_PEDAL_LENGTH;

    public enum StripKind {
        PREVIEW,
        BELL
    }

    private ArenaGridParser() {}

    public record NormalizedRect(
            World world,
            int minBlockX,
            int minBlockZ,
            int sizeX,
            int sizeZ,
            double y,
            boolean transposed) {

        public int columnSpan() {
            return transposed ? sizeZ : sizeX;
        }

        public int rowSpan() {
            return transposed ? sizeX : sizeZ;
        }

        public int maxBlockX() {
            return minBlockX + sizeX - 1;
        }

        public int maxBlockZ() {
            return minBlockZ + sizeZ - 1;
        }

        public Location cellCenter(int col, int row) {
            if (transposed) {
                return new Location(world, minBlockX + row + 0.5, y, minBlockZ + col + 0.5);
            }
            return new Location(world, minBlockX + col + 0.5, y, minBlockZ + row + 0.5);
        }
    }

    public static NormalizedRect normalize(Location cornerA, Location cornerB) {
        if (cornerA == null || cornerB == null) {
            return null;
        }
        World world = cornerA.getWorld();
        if (world == null || cornerB.getWorld() == null || !world.getUID().equals(cornerB.getWorld().getUID())) {
            return null;
        }
        int minX = Math.min(cornerA.getBlockX(), cornerB.getBlockX());
        int maxX = Math.max(cornerA.getBlockX(), cornerB.getBlockX());
        int minZ = Math.min(cornerA.getBlockZ(), cornerB.getBlockZ());
        int maxZ = Math.max(cornerA.getBlockZ(), cornerB.getBlockZ());
        return new NormalizedRect(world, minX, minZ, maxX - minX + 1, maxZ - minZ + 1, cornerA.getY(), false);
    }

    public static NormalizedRect normalizeForGrid(
            Location cornerA, Location cornerB, int expectedCols, int expectedRows) {
        NormalizedRect raw = normalize(cornerA, cornerB);
        if (raw == null) {
            return null;
        }
        int dx = raw.sizeX();
        int dz = raw.sizeZ();
        if (dx == expectedCols && dz == expectedRows) {
            return raw;
        }
        if (dx == expectedRows && dz == expectedCols) {
            return new NormalizedRect(raw.world(), raw.minBlockX(), raw.minBlockZ(), dx, dz, raw.y(), true);
        }
        return raw;
    }

    /** 2×11 条带中心（预览条 row0-1，敲钟条 row15-16）。 */
    public static Location stripCenter(NormalizedRect rect, StripKind kind, ArenaOrientation orientation) {
        if (rect == null) {
            return null;
        }
        double centerAlongWidth = (ARENA_COLS - 1) / 2.0 + 0.5;
        double localRowCenter =
                switch (kind) {
                    // 2 格条带：行中心 0.5 与 1.5（或 bell 段对应行），取几何中点而非 +1.5
                    case PREVIEW -> (STRIP_ROWS - 1) / 2.0 + 0.5;
                    case BELL -> BELL_STRIP_START + (STRIP_ROWS - 1) / 2.0 + 0.5;
                };
        return mapLocal(rect, centerAlongWidth, localRowCenter, orientation);
    }

    /** 2×2 槽位中心（逻辑 col/row 均为 0..3，位于主网格内）。 */
    public static Location slotCenter(NormalizedRect rect, int col, int row, ArenaOrientation orientation) {
        if (rect == null || col < 0 || col >= SLOT_COUNT_PER_ROW || row < 0 || row >= SLOT_COUNT_PER_ROW) {
            return null;
        }
        double localX = col * SLOT_STRIDE + 1.0;
        double localZ = MAIN_GRID_START + row * SLOT_STRIDE + 1.0;
        return mapLocal(rect, localX, localZ, orientation);
    }

    private static Location mapLocal(
            NormalizedRect rect, double localX, double localZ, ArenaOrientation orientation) {
        return switch (orientation) {
            case SOUTH -> new Location(
                    rect.world(), rect.minBlockX() + localX, rect.y(), rect.minBlockZ() + localZ);
            case NORTH -> new Location(
                    rect.world(), rect.minBlockX() + localX, rect.y(), rect.maxBlockZ() - localZ + 1.0);
            case EAST -> new Location(
                    rect.world(), rect.minBlockX() + localZ, rect.y(), rect.minBlockZ() + localX);
            case WEST -> new Location(
                    rect.world(), rect.maxBlockX() - localZ + 1.0, rect.y(), rect.minBlockZ() + localX);
        };
    }

    public static Location[] parseSlotRow(NormalizedRect rect, int row, boolean mirrorX) {
        if (rect == null || row < 0 || row >= rect.rowSpan()) {
            return emptySlots();
        }
        Location[] slots = new Location[SLOT_COUNT_PER_ROW];
        int[] columns = {0, 2, 4, 6};
        for (int i = 0; i < columns.length; i++) {
            int col = mirrorX ? rect.columnSpan() - 1 - columns[i] : columns[i];
            slots[i] = rect.cellCenter(col, row);
        }
        return slots;
    }

    public static Location[] parseLogicalRow(NormalizedRect rect, int logicalRow, ArenaOrientation orientation) {
        Location[] slots = new Location[SLOT_COUNT_PER_ROW];
        for (int col = 0; col < SLOT_COUNT_PER_ROW; col++) {
            slots[col] = slotCenter(rect, col, logicalRow, orientation);
        }
        return slots;
    }

    public static Location[] emptySlots() {
        return new Location[] {null, null, null, null};
    }

    public static int mirroredColumn(NormalizedRect rect, int col, boolean mirrorX) {
        return mirrorX ? rect.columnSpan() - 1 - col : col;
    }
}
