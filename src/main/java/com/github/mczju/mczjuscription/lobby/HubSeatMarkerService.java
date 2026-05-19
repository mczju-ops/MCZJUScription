package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * 大厅座位右键范围示意（全服每房间一套，非按玩家）。
 * <p>
 * 键为 {@code gameId/roomName}；若已有存活实体则跳过。生成前会清理同房间遗留的 tagged 实体，避免重载后叠层。
 */
public final class HubSeatMarkerService {

    public static final float MARKER_SCALE = 0.8f;
    private static final double ORPHAN_SCAN_RADIUS = 2.5;

    private static final BlockData GLASS = Material.WHITE_STAINED_GLASS.createBlockData();
    private static final Map<String, List<UUID>> MARKERS = new ConcurrentHashMap<>();
    private static final Map<String, Object> LOCKS = new ConcurrentHashMap<>();

    private HubSeatMarkerService() {}

    /** 确保本房间座位示意存在（多人进大厅也只会维护一套）。 */
    public static void ensureSpawned(InscriptionGameRoom room) {
        if (room == null) {
            return;
        }
        String key = roomKey(room);
        Object lock = LOCKS.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            List<UUID> existing = MARKERS.get(key);
            if (existing != null && !existing.isEmpty() && allAlive(existing)) {
                return;
            }
            despawn(room);
            removeOrphanMarkers(room);
            List<UUID> spawned = new ArrayList<>();
            for (Location seat : HubLocationUtil.seatInteractionPoints(room)) {
                BlockDisplay display = spawnMarker(seat);
                if (display != null) {
                    spawned.add(display.getUniqueId());
                }
            }
            if (!spawned.isEmpty()) {
                MARKERS.put(key, spawned);
            }
        }
    }

    public static void despawn(InscriptionGameRoom room) {
        if (room == null) {
            return;
        }
        String key = roomKey(room);
        synchronized (LOCKS.computeIfAbsent(key, k -> new Object())) {
            List<UUID> ids = MARKERS.remove(key);
            if (ids == null) {
                return;
            }
            for (UUID id : ids) {
                Entity entity = findEntity(id);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
    }

    private static void removeOrphanMarkers(InscriptionGameRoom room) {
        for (Location seat : HubLocationUtil.seatInteractionPoints(room)) {
            if (seat.getWorld() == null) {
                continue;
            }
            for (Entity entity :
                    seat.getWorld().getNearbyEntities(seat, ORPHAN_SCAN_RADIUS, ORPHAN_SCAN_RADIUS, ORPHAN_SCAN_RADIUS)) {
                if (entity instanceof BlockDisplay display && isSeatMarker(display)) {
                    display.remove();
                }
            }
        }
    }

    private static boolean isSeatMarker(Entity entity) {
        if (InscriptionKeys.HUB_SEAT_MARKER == null) {
            return false;
        }
        return entity.getPersistentDataContainer().has(InscriptionKeys.HUB_SEAT_MARKER, PersistentDataType.BYTE);
    }

    private static BlockDisplay spawnMarker(Location seatConfigured) {
        Location spawn = HubLocationUtil.seatMarkerSpawn(seatConfigured, MARKER_SCALE);
        if (spawn == null || spawn.getWorld() == null) {
            return null;
        }
        return spawn.getWorld()
                .spawn(
                        spawn,
                        BlockDisplay.class,
                        display -> {
                            display.setBlock(GLASS);
                            display.setBillboard(Display.Billboard.FIXED);
                            display.setTransformation(markerTransform());
                            display.setBrightness(new Display.Brightness(15, 15));
                            display.setInvulnerable(true);
                            display.setPersistent(true);
                            display
                                    .getPersistentDataContainer()
                                    .set(InscriptionKeys.HUB_SEAT_MARKER, PersistentDataType.BYTE, (byte) 1);
                        });
    }

    private static Transformation markerTransform() {
        float s = MARKER_SCALE;
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(s, s, s),
                new AxisAngle4f(0f, 0f, 0f, 1f));
    }

    private static boolean allAlive(List<UUID> ids) {
        for (UUID id : ids) {
            if (findEntity(id) == null) {
                return false;
            }
        }
        return true;
    }

    private static Entity findEntity(UUID id) {
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static String roomKey(InscriptionGameRoom room) {
        return room.getGameId() + "/" + room.getRoomName();
    }
}
