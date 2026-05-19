package com.github.mczju.mczjuscription.vfx;

import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/** 棋盘造物攻击、移动、生成等粒子效果。 */
public final class BoardVfx {

    private BoardVfx() {}

    public static void playSpawn(Location loc) {
        Location at = center(loc);
        if (at == null) return;
        World world = at.getWorld();
        world.spawnParticle(Particle.POOF, at, 18, 0.35, 0.45, 0.35, 0.04);
        world.spawnParticle(Particle.HAPPY_VILLAGER, at.clone().add(0, 0.8, 0), 6, 0.15, 0.2, 0.15, 0);
    }

    public static void playMove(Location from, Location to) {
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
        Vector step = delta.multiply(1.0 / Math.max(1, (int) (distance * 5)));
        Location cursor = start.clone();
        int steps = (int) Math.max(4, distance * 5);
        for (int i = 0; i <= steps; i++) {
            world.spawnParticle(
                    Particle.CLOUD,
                    cursor.clone().add(0, 0.45, 0),
                    2,
                    0.04, 0.05, 0.04,
                    0.01
            );
            cursor.add(step);
        }
        world.spawnParticle(Particle.POOF, end.clone().add(0, 0.2, 0), 10, 0.2, 0.25, 0.2, 0.03);
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
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, at, 20, 0.35, 0.5, 0.35, 0.02);
        }
    }

    public static void playDirectDamage(InscriptionMatch match, MatchSide victimSide, int damage) {
        Location at = resolveDirectDamageLocation(match, victimSide);
        if (at == null) return;
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
        world.spawnParticle(Particle.SMOKE, at, 16, 0.35, 0.45, 0.35, 0.03);
        world.spawnParticle(Particle.ASH, at.clone().add(0, 0.3, 0), 12, 0.2, 0.3, 0.2, 0.02);
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

    private static Location resolveDirectDamageLocation(InscriptionMatch match, MatchSide victimSide) {
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
