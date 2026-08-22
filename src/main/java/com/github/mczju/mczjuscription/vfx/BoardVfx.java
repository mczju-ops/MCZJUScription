package com.github.mczju.mczjuscription.vfx;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** 棋盘造物攻击、移动、生成等粒子效果。 */
public final class BoardVfx {

    private static final int SACRIFICE_DROP_LIFETIME_TICKS = 50;

    private BoardVfx() {}

    public static void playSpawn(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.spawnParticle(Particle.POOF, at, 18, 0.35, 0.45, 0.35, 0.04);
        world.spawnParticle(Particle.HAPPY_VILLAGER, at.clone().add(0, 0.8, 0), 6, 0.15, 0.2, 0.15, 0);
    }

    /** 末影人【穿梭】：出发/到达时的传送音效与粒子。 */
    public static void playEnderTeleport(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.playSound(at, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        world.spawnParticle(Particle.PORTAL, at.clone().add(0, 0.55, 0), 28, 0.4, 0.55, 0.4, 0.45);
        world.spawnParticle(Particle.REVERSE_PORTAL, at.clone().add(0, 0.65, 0), 10, 0.18, 0.3, 0.18, 0.04);
    }

    public static void playMove(Location from, Location to) {
        playMove(from, to, 12);
    }

    /** 沿路径逐 tick 播放尾迹，{@code durationTicks} 应与造物平移时长一致。 */
    public static void playMove(Location from, Location to, int durationTicks) {
        playTrail(from, to, durationTicks, Particle.CLOUD, 0.35, 0.004);
    }

    /** 【蛮力】推挤：尘土尾迹 + 落地冲击。 */
    public static void playPushTrail(Location from, Location to, int durationTicks) {
        playTrail(from, to, durationTicks, Particle.CRIT, 0.42, 0.012);
    }

    public static void playPushImpact(Location at) {
        Location center = center(at);
        if (center == null) return;
        World world = center.getWorld();
        world.playSound(center, Sound.ENTITY_RAVAGER_STEP, 0.55f, 1.35f);
        world.spawnParticle(Particle.POOF, center.clone().add(0, 0.25, 0), 10, 0.18, 0.12, 0.18, 0.02);
        world.spawnParticle(Particle.CLOUD, center.clone().add(0, 0.35, 0), 6, 0.12, 0.08, 0.12, 0.01);
        world.spawnParticle(Particle.SWEEP_ATTACK, center.clone().add(0, 0.45, 0), 1, 0, 0, 0, 0);
    }

    private static void playTrail(
            Location from,
            Location to,
            int durationTicks,
            Particle particle,
            double yOffset,
            double speed) {
        Location start = center(from);
        Location end = center(to);
        if (start == null || end == null || start.getWorld() == null) return;
        World world = start.getWorld();
        if (!world.equals(end.getWorld())) return;

        Vector delta = end.toVector().subtract(start.toVector());
        double distance = delta.length();
        if (distance < 0.05) {
            playSpawn(end);
            return;
        }

        int ticks = Math.max(4, durationTicks);
        Vector step = delta.multiply(1.0 / ticks);
        Location cursor = start.clone();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= ticks) {
                    world.spawnParticle(Particle.POOF, end.clone().add(0, 0.2, 0), 3, 0.08, 0.1, 0.08, 0.008);
                    cancel();
                    return;
                }
                world.spawnParticle(
                        particle,
                        cursor.clone().add(0, yOffset, 0),
                        1,
                        0.02, 0.03, 0.02,
                        speed);
                cursor.add(step);
                tick++;
            }
        }.runTaskTimer(MCZJUScriptionPlugin.getInstance(), 0L, 1L);
    }

    /** 献祭：掉落骨与腐肉展示物，稍后消失。 */
    public static void playSacrificeDrops(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.playSound(at, Sound.ENTITY_ITEM_PICKUP, 0.55f, 0.85f);
        playDeath(at);
        spawnCosmeticDrop(world, at, Material.BONE, -0.06, 0.12, -0.04);
        spawnCosmeticDrop(world, at, Material.ROTTEN_FLESH, 0.06, 0.14, 0.04);
    }

    public static boolean isCosmeticDrop(Item item) {
        if (item == null) return false;
        return item.getPersistentDataContainer().has(InscriptionKeys.COSMETIC_DROP, PersistentDataType.BYTE);
    }

    private static void spawnCosmeticDrop(
            World world, Location at, Material material, double vx, double vy, double vz) {
        ItemStack stack = new ItemStack(material, 1);
        Item drop = world.dropItem(at.clone().add(0, 0.55, 0), stack);
        drop.setVelocity(new Vector(vx, vy, vz));
        drop.setPickupDelay(Integer.MAX_VALUE);
        drop.setCanMobPickup(false);
        drop.getPersistentDataContainer().set(InscriptionKeys.COSMETIC_DROP, PersistentDataType.BYTE, (byte) 1);
        Bukkit.getScheduler()
                .runTaskLater(
                        MCZJUScriptionPlugin.getInstance(),
                        () -> {
                            if (drop.isValid()) {
                                drop.remove();
                            }
                        },
                        SACRIFICE_DROP_LIFETIME_TICKS);
    }

    public static void playAttackAt(Location loc, int damage, boolean instantKill) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        int n = Math.min(24, 8 + damage * 3);
        world.spawnParticle(Particle.CRIT, at, n, 0.4, 0.55, 0.4, 0.12);
        world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                at.clone().add(0, 1.0, 0),
                Math.min(12, Math.max(3, damage)),
                0.25, 0.35, 0.25,
                0
        );
        world.spawnParticle(Particle.SWEEP_ATTACK, at, 1);
        if (instantKill) {
            world.spawnParticle(Particle.TRIAL_OMEN, at, 20, 0.35, 0.5, 0.35, 0.02);
        }
    }

    public static void playDirectDamage(
            InscriptionMatch match, MatchSide victimSide, int lane, int damage) {
        Location at = slotLocation(match, victimSide, lane);
        if (at == null) {
            at = resolveDirectDamageFallback(match, victimSide);
        }
        if (at == null) return;
        at = center(at);
        World world = at.getWorld();
        world.spawnParticle(Particle.CRIT, at, Math.min(30, 10 + damage * 4), 0.5, 0.6, 0.5, 0.15);
        world.spawnParticle(Particle.SMOKE, at.clone().add(0, 0.5, 0), 12, 0.3, 0.4, 0.3, 0.02);
    }

    public static void playTransform(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 28, 0.4, 0.6, 0.4, 0.03);
        world.spawnParticle(Particle.POOF, at, 14, 0.3, 0.4, 0.3, 0.05);
    }

    public static void playDeath(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.spawnParticle(Particle.SMOKE, at, 8, 0.2, 0.28, 0.2, 0.015);
        world.spawnParticle(Particle.ASH, at.clone().add(0, 0.3, 0), 6, 0.12, 0.18, 0.12, 0.01);
    }

    /** 【自爆】主体：大爆炸 + 音效。 */
    public static void playSelfDestructMain(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.82f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, at.clone().add(0, 0.35, 0), 1);
        world.spawnParticle(Particle.FLAME, at, 48, 0.55, 0.7, 0.55, 0.06);
        world.spawnParticle(Particle.LARGE_SMOKE, at.clone().add(0, 0.45, 0), 20, 0.5, 0.6, 0.5, 0.04);
        world.spawnParticle(Particle.SMOKE, at, 28, 0.45, 0.55, 0.45, 0.05);
    }

    /** 【自爆】溅射：面前/相邻格较小爆炸。 */
    public static void playSelfDestructSplash(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.4f);
        world.spawnParticle(Particle.EXPLOSION, at.clone().add(0, 0.25, 0), 1, 0.05, 0.08, 0.05, 0);
        world.spawnParticle(Particle.FLAME, at, 18, 0.28, 0.35, 0.28, 0.03);
        world.spawnParticle(Particle.SMOKE, at, 10, 0.22, 0.28, 0.22, 0.02);
    }

    public static Location locationOf(InscriptionMatch match, BoardCreature creature) {
        if (creature == null) return null;
        if (match.arena() != null && creature.slot() != null) {
            Location slot = match.arena().slotLocation(creature.slot().owner(), creature.slot().index());
            if (slot != null) return slot;
        }
        if (creature.entityId() != null) {
            Entity entity = Bukkit.getEntity(creature.entityId());
            if (entity != null) return entity.getLocation();
        }
        return null;
    }

    public static Location slotLocation(InscriptionMatch match, MatchSide side, int laneIndex) {
        BattleArena arena = match.arena();
        if (arena == null) return null;
        SlotOwner row = side == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
        return arena.slotLocation(row, laneIndex);
    }

    public static Location slotLocation(InscriptionMatch match, SlotOwner owner, int laneIndex) {
        BattleArena arena = match.arena();
        if (arena == null) return null;
        return arena.slotLocation(owner, laneIndex);
    }

    private static Location resolveDirectDamageFallback(InscriptionMatch match, MatchSide victimSide) {
        BattleArena arena = match.arena();
        if (arena != null) {
            SlotOwner row = victimSide == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
            Location mid = arena.slotLocation(row, 1);
            if (mid == null) {
                mid = arena.slotLocation(row, 0);
            }
            if (mid != null) return mid;
        }
        return match.humanParticipants().stream()
                .findFirst()
                .flatMap(h -> h.player().map(p -> p.player().getLocation()))
                .orElse(null);
    }

    private static Location center(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        return loc.clone().add(0, 0.15, 0);
    }
}
