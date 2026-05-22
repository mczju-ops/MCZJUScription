package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import org.bukkit.Location;

/** 将房间 JSON 解析为槽位坐标（11×17 统一场地 + 朝向）。 */
public final class ArenaLayoutResolver {

    /** 主网格逻辑行：0=敌后场，1=敌站场，2=我站场，3=UI整备。 */
    private static final int ROW_BACK = 0;
    private static final int ROW_ENEMY = 1;
    private static final int ROW_PLAYER = 2;
    private static final int ROW_UI = 3;

    private ArenaLayoutResolver() {}

    public static ResolvedArenaLayout resolve(InscriptionGameRoom room, MatchMode mode) {
        if (!room.hasUnifiedArena()) {
            throw new IllegalStateException("未配置 arenaCornerA / arenaCornerB（11×17 统一场地）");
        }
        return resolveUnified(room, mode);
    }

    private static ResolvedArenaLayout resolveUnified(InscriptionGameRoom room, MatchMode mode) {
        ArenaGridParser.NormalizedRect arena = requireRect(
                room.arenaCornerA,
                room.arenaCornerB,
                ArenaGridParser.ARENA_COLS,
                ArenaGridParser.ARENA_ROWS,
                "场地");
        ArenaOrientation orientation = room.arenaOrientation();

        Location previewStripCenter =
                ArenaGridParser.stripCenter(arena, ArenaGridParser.StripKind.PREVIEW, orientation);
        Location playerBellStripCenter =
                ArenaGridParser.stripCenter(arena, ArenaGridParser.StripKind.BELL, orientation);
        Location enemyBellStripCenter = null;
        if (mode == MatchMode.DUEL_PVP) {
            enemyBellStripCenter = previewStripCenter;
            previewStripCenter = null;
        }

        Location[] previewSlots = ArenaGridParser.parseLogicalRow(arena, ROW_BACK, orientation);
        Location[] enemyUiSlots = ArenaGridParser.emptySlots();
        if (mode == MatchMode.DUEL_PVP) {
            enemyUiSlots = ArenaGridParser.parseLogicalRow(arena, ROW_BACK, orientation);
            previewSlots = ArenaGridParser.emptySlots();
        }

        Location[] enemySlots = ArenaGridParser.parseLogicalRow(arena, ROW_ENEMY, orientation);
        Location[] playerSlots = ArenaGridParser.parseLogicalRow(arena, ROW_PLAYER, orientation);
        Location[] playerUiSlots = ArenaGridParser.parseLogicalRow(arena, ROW_UI, orientation);

        ResolvedArenaLayout.StagingSites playerStaging =
                buildUnifiedStaging(arena, orientation, ROW_UI, playerBellStripCenter);
        ResolvedArenaLayout.StagingSites enemyStaging = null;
        if (mode == MatchMode.DUEL_PVP) {
            enemyStaging = buildUnifiedStaging(arena, orientation, ROW_BACK, enemyBellStripCenter);
        }

        return new ResolvedArenaLayout(
                playerSlots,
                enemySlots,
                previewSlots,
                playerUiSlots,
                enemyUiSlots,
                previewStripCenter,
                playerBellStripCenter,
                enemyBellStripCenter,
                playerStaging,
                enemyStaging);
    }

    private static ResolvedArenaLayout.StagingSites buildUnifiedStaging(
            ArenaGridParser.NormalizedRect arena,
            ArenaOrientation orientation,
            int uiRow,
            Location bellStripCenter) {
        ResolvedArenaLayout.StagingSites sites = new ResolvedArenaLayout.StagingSites();
        Location uiLeft = ArenaGridParser.slotCenter(arena, 0, uiRow, orientation);
        if (bellStripCenter != null) {
            sites.bellStripCenter = bellStripCenter.clone();
            sites.playerSpawn = offsetTowardBellStrip(bellStripCenter, orientation, -2.5);
        }
        if (uiLeft != null) {
            sites.wanderingTrader = uiLeft.clone();
        }
        sites.shopSlot = ArenaGridParser.slotCenter(arena, 3, uiRow, orientation);
        return sites;
    }

    private static Location offsetTowardBellStrip(
            Location bellStripCenter, ArenaOrientation orientation, double blocks) {
        if (bellStripCenter == null) {
            return null;
        }
        return switch (orientation) {
            case SOUTH -> bellStripCenter.clone().add(0, 0, blocks);
            case NORTH -> bellStripCenter.clone().add(0, 0, -blocks);
            case EAST -> bellStripCenter.clone().add(blocks, 0, 0);
            case WEST -> bellStripCenter.clone().add(-blocks, 0, 0);
        };
    }

    private static ArenaGridParser.NormalizedRect requireRect(
            Location cornerA,
            Location cornerB,
            int expectedCols,
            int expectedRows,
            String label) {
        ArenaGridParser.NormalizedRect rect =
                ArenaGridParser.normalizeForGrid(cornerA, cornerB, expectedCols, expectedRows);
        if (rect == null) {
            throw new IllegalStateException(label + " 角点无效或不在同一 world");
        }
        if (rect.columnSpan() != expectedCols || rect.rowSpan() != expectedRows) {
            throw new IllegalStateException(
                    label + " 角点矩形应为 %d×%d 或 %d×%d 格（X×Z），实际为 X %d 格、Z %d 格"
                            .formatted(
                                    expectedCols,
                                    expectedRows,
                                    expectedRows,
                                    expectedCols,
                                    rect.sizeX(),
                                    rect.sizeZ()));
        }
        return rect;
    }
}
