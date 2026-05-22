package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

/** 根据棋盘相对位置计算造物朝向。 */
public final class ArenaFacing {

    private ArenaFacing() {}

    /** 己方造物朝敌方，敌方/预览区造物朝己方。 */
    public static Location facingTarget(BattleArena arena, SlotOwner owner, int slotIndex) {
        if (arena == null) return null;
        SlotOwner opponent = switch (owner) {
            case PLAYER -> SlotOwner.ENEMY;
            case ENEMY, ENEMY_PREVIEW -> SlotOwner.PLAYER;
        };
        for (int i : new int[] {slotIndex, 1, 0, 2, 3}) {
            if (i < 0 || i >= BoardSlot.SLOT_COUNT) continue;
            Location loc = arena.slotLocation(opponent, i);
            if (loc != null) return loc;
        }
        return null;
    }

    public static float yawFacing(Location from, Location toward) {
        double dx = toward.getX() - from.getX();
        double dz = toward.getZ() - from.getZ();
        if (dx * dx + dz * dz < 1e-8) return 0f;
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    public static Location withYawToward(Location at, Location toward) {
        Location result = at.clone();
        if (toward == null || at.getWorld() == null || !at.getWorld().equals(toward.getWorld())) {
            return result;
        }
        result.setYaw(yawFacing(at, toward));
        result.setPitch(0f);
        return result;
    }

    public static void applyFacing(LivingEntity entity, Location toward) {
        if (entity == null || toward == null || !entity.isValid()) return;
        Location at = entity.getLocation();
        entity.teleport(withYawToward(at, toward));
    }

    /** 仅调整朝向，不改变实体位置（适用于 AI 关闭的展示用村民等）。 */
    public static void faceToward(LivingEntity entity, Location toward) {
        if (entity == null || toward == null || !entity.isValid()) {
            return;
        }
        Location from = entity.getEyeLocation();
        if (from.getWorld() == null
                || toward.getWorld() == null
                || !from.getWorld().equals(toward.getWorld())) {
            return;
        }
        double dx = toward.getX() - from.getX();
        double dy = toward.getY() - from.getY();
        double dz = toward.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1e-8 && Math.abs(dy) < 1e-8) {
            return;
        }
        float yaw = horizontal < 1e-8
                ? entity.getLocation().getYaw()
                : (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, horizontal));
        entity.setRotation(yaw, pitch);
    }
}
