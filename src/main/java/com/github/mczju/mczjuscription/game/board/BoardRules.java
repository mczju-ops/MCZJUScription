package com.github.mczju.mczjuscription.game.board;

/** 槽位规则：任意空槽可放置，不要求连续占满。 */
public final class BoardRules {

    private BoardRules() {}

    public static boolean canPlaceAt(BoardSlot[] row, int index) {
        if (index < 0 || index >= BoardSlot.SLOT_COUNT) return false;
        return row[index].isEmpty();
    }

    /** 从 startIndex 起环形查找第一个空槽；-1 表示已满。 */
    public static int nextEmptyIndex(BoardSlot[] row, int startIndex) {
        for (int offset = 0; offset < BoardSlot.SLOT_COUNT; offset++) {
            int i = (startIndex + offset) % BoardSlot.SLOT_COUNT;
            if (row[i].isEmpty()) return i;
        }
        return -1;
    }

    public static boolean canAdvancePreviewToCombat(BoardSlot[] combatRow, int index) {
        if (index < 0 || index >= BoardSlot.SLOT_COUNT) return false;
        return combatRow[index].isEmpty();
    }
}
