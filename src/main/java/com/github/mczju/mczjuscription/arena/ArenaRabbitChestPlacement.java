package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.match.MatchSide;
import org.bukkit.Location;
import org.bukkit.block.Block;

/** @deprecated 兔子堆已移除，保留类避免旧引用编译失败。 */
@Deprecated
public final class ArenaRabbitChestPlacement {

    private ArenaRabbitChestPlacement() {}

    public static boolean isRabbitChestBlock(BattleArena arena, Block block, MatchSide side) {
        return false;
    }

    public static boolean isRabbitChestInteract(
            BattleArena arena, Location point, MatchSide side) {
        return false;
    }
}
