package com.github.mczju.mczjuscription.arena;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** 玩家视线数学射线命中场地踏板。 */
public final class ArenaSlotRay {

    private ArenaSlotRay() {}

    public static ArenaPedalTarget tracePedal(Player player, BattleArena arena) {
        if (player == null || arena == null) {
            return null;
        }
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        return ArenaPedalRaycast.trace(arena, eye, direction);
    }
}
