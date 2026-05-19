package com.github.mczju.mczjuscription.arena;

import org.bukkit.Location;
import org.bukkit.block.Block;

/** 判断玩家交互是否落在房间配置的钟方块上。 */
public final class ArenaClockPlacement {

    private ArenaClockPlacement() {}

    public static boolean isClockBlock(BattleArena arena, Block block) {
        return arena != null && arena.isClockBlock(block);
    }

    public static boolean isClockInteract(Location interactPoint, Location clockCenter) {
        if (interactPoint == null || clockCenter == null || interactPoint.getWorld() == null) {
            return false;
        }
        return ArenaSlotPlacement.isOnSlotBlock(interactPoint, clockCenter);
    }
}
