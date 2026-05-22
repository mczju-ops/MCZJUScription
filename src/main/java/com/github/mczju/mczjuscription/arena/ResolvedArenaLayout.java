package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.match.MatchSide;
import org.bukkit.Location;

/** 从房间矩形配置解析出的全部场地坐标。 */
public final class ResolvedArenaLayout {

    private final Location[] playerSlots;
    private final Location[] enemySlots;
    private final Location[] previewSlots;
    private final Location[] playerUiSlots;
    private final Location[] enemyUiSlots;
    private final Location previewStripCenter;
    private final Location playerBellStripCenter;
    private final Location enemyBellStripCenter;
    private final StagingSites playerStaging;
    private final StagingSites enemyStaging;

    public ResolvedArenaLayout(
            Location[] playerSlots,
            Location[] enemySlots,
            Location[] previewSlots,
            Location[] playerUiSlots,
            Location[] enemyUiSlots,
            Location previewStripCenter,
            Location playerBellStripCenter,
            Location enemyBellStripCenter,
            StagingSites playerStaging,
            StagingSites enemyStaging) {
        this.playerSlots = copySlots(playerSlots);
        this.enemySlots = copySlots(enemySlots);
        this.previewSlots = copySlots(previewSlots);
        this.playerUiSlots = copySlots(playerUiSlots);
        this.enemyUiSlots = copySlots(enemyUiSlots);
        this.previewStripCenter = clone(previewStripCenter);
        this.playerBellStripCenter = clone(playerBellStripCenter);
        this.enemyBellStripCenter = clone(enemyBellStripCenter);
        this.playerStaging = playerStaging;
        this.enemyStaging = enemyStaging;
    }

    public Location slot(com.github.mczju.mczjuscription.game.board.SlotOwner owner, int index) {
        if (index < 0 || index >= 4) {
            return null;
        }
        return switch (owner) {
            case PLAYER -> clone(playerSlots[index]);
            case ENEMY -> clone(enemySlots[index]);
            case ENEMY_PREVIEW -> clone(previewSlots[index]);
        };
    }

    public Location uiSlot(MatchSide side, UiSlotKind kind) {
        Location[] row = side == MatchSide.PLAYER ? playerUiSlots : enemyUiSlots;
        if (row == null || kind.index() < 0 || kind.index() >= row.length) {
            return null;
        }
        return clone(row[kind.index()]);
    }

    public Location previewStripCenter() {
        return clone(previewStripCenter);
    }

    public Location bellStripCenter(MatchSide side) {
        return side == MatchSide.PLAYER ? clone(playerBellStripCenter) : clone(enemyBellStripCenter);
    }

    public boolean hasPreviewStrip() {
        return previewStripCenter != null;
    }

    public boolean hasBellStrip(MatchSide side) {
        return bellStripCenter(side) != null;
    }

    public boolean hasUiRow(MatchSide side) {
        Location[] row = side == MatchSide.PLAYER ? playerUiSlots : enemyUiSlots;
        return row != null && row[0] != null;
    }

    public StagingSites staging(MatchSide side) {
        return side == MatchSide.PLAYER ? playerStaging : enemyStaging;
    }

    public boolean hasPreview() {
        return previewSlots[0] != null;
    }

    public enum UiSlotKind {
        BLOOD(0),
        BONES(1),
        FISH(2),
        SHOP(3);

        private final int index;

        UiSlotKind(int index) {
            this.index = index;
        }

        public int index() {
            return index;
        }

        public static UiSlotKind fromIndex(int index) {
            for (UiSlotKind kind : values()) {
                if (kind.index == index) {
                    return kind;
                }
            }
            return null;
        }
    }

    public static final class StagingSites {
        public Location clock;
        public Location shopVillager;
        public Location wanderingTrader;
        public Location playerSpawn;
        public Location shopSlot;
        public Location bellStripCenter;
    }

    private static Location[] copySlots(Location[] source) {
        Location[] copy = new Location[4];
        if (source != null) {
            for (int i = 0; i < 4; i++) {
                copy[i] = clone(source[i]);
            }
        }
        return copy;
    }

    private static Location clone(Location loc) {
        return loc == null ? null : loc.clone();
    }
}
