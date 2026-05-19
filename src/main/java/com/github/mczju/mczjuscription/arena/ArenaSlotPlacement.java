package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import org.bukkit.Location;

import java.util.OptionalInt;

/** 根据掉落物位置判断落在哪个槽位方块上。 */
public final class ArenaSlotPlacement {

    private ArenaSlotPlacement() {}

    public static OptionalInt findSlotIndex(BattleArena arena, Location dropLocation, SlotOwner row) {
        if (arena == null || dropLocation == null || dropLocation.getWorld() == null) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
            Location slotCenter = arena.slotLocation(row, i);
            if (slotCenter == null) continue;
            if (!dropLocation.getWorld().equals(slotCenter.getWorld())) continue;
            if (isOnSlotBlock(dropLocation, slotCenter)) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /** 掉落物在槽位中心附近（半径略大于踏板半宽 {@link ArenaCoordinates#PEDAL_HALF_WIDTH}）。 */
    public static boolean isOnSlotBlock(Location dropLocation, Location slotCenter) {
        double dx = dropLocation.getX() - slotCenter.getX();
        double dz = dropLocation.getZ() - slotCenter.getZ();
        double radius = ArenaCoordinates.PEDAL_HALF_WIDTH + 0.05;
        if (dx * dx + dz * dz > radius * radius) return false;
        return Math.abs(dropLocation.getY() - slotCenter.getY()) <= 2.0;
    }
}
