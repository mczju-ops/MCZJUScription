package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.match.MatchSide;
import org.bukkit.Location;
import org.bukkit.block.Block;

/** 判断玩家交互是否落在敲钟方块上。 */
public final class ArenaClockPlacement {

    private ArenaClockPlacement() {}

    public static boolean isClockBlock(BattleArena arena, Block block, MatchSide side) {
        return arena != null && side != null && arena.isClockBlock(block, side);
    }

    public static boolean isClockInteract(Location interactPoint, Location clockCenter) {
        if (interactPoint == null || clockCenter == null || interactPoint.getWorld() == null) {
            return false;
        }
        return ArenaSlotPlacement.isOnSlotBlock(interactPoint, clockCenter);
    }
}
