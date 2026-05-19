package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bell;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BattleArena {

    private static final float PEDAL_THICKNESS = 0.1f;

    private final UUID arenaId = UUID.randomUUID();
    private final UUID ownerId;
    private final Map<SlotKey, UUID> pedalMarkers = new HashMap<>();
    private final Map<SlotKey, Location> slotLocations = new HashMap<>();
    /** 敲钟交互中心（方块中心 +0.5）。 */
    private Location clockLocation;
    private Location clockBlockLocation;
    private BlockData clockPreviousData;
    private boolean clockPlacedByArena;

    private BattleArena(UUID ownerId) {
        this.ownerId = ownerId;
    }

    /** 在玩家脚下按朝向生成临时场地（调试用）。 */
    public static BattleArena atPlayer(Player player) {
        BattleArena arena = new BattleArena(player.getUniqueId());
        arena.buildRelativeTo(player);
        return arena;
    }

    /** 从房间 JSON 配置的槽位坐标生成场地。 */
    public static BattleArena fromRoom(InscriptionGameRoom room) {
        BattleArena arena = new BattleArena(UUID.randomUUID());
        for (SlotOwner owner : SlotOwner.values()) {
            for (int i = 0; i < 4; i++) {
                Location loc = room.slotLocation(owner, i);
                if (loc != null) {
                    arena.placeMarker(owner, i, loc);
                }
            }
        }
        if (room.clockAt != null) {
            arena.placeClockBlock(room.clockAt);
        }
        return arena;
    }

    public UUID arenaId() {
        return arenaId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Location slotLocation(SlotOwner owner, int index) {
        return slotLocations.get(new SlotKey(owner, index));
    }

    public Location clockLocation() {
        return clockLocation == null ? null : clockLocation.clone();
    }

    public boolean hasClock() {
        return clockLocation != null;
    }

    public boolean isClockBlock(Block block) {
        if (block == null || clockBlockLocation == null) return false;
        Location at = block.getLocation();
        return at.getBlockX() == clockBlockLocation.getBlockX()
                && at.getBlockY() == clockBlockLocation.getBlockY()
                && at.getBlockZ() == clockBlockLocation.getBlockZ()
                && at.getWorld().equals(clockBlockLocation.getWorld());
    }

    public SlotOwner resolveOwner(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(InscriptionKeys.SLOT_OWNER, PersistentDataType.STRING);
        if (raw == null) return null;
        return SlotOwner.valueOf(raw);
    }

    public int resolveIndex(Entity entity) {
        Integer idx = entity.getPersistentDataContainer().get(InscriptionKeys.SLOT_INDEX, PersistentDataType.INTEGER);
        return idx == null ? -1 : idx;
    }

    public boolean isSlotMarker(Entity entity) {
        if (!(entity instanceof Display)) return false;
        return arenaId.toString().equals(
                entity.getPersistentDataContainer().get(InscriptionKeys.ARENA_ID, PersistentDataType.STRING)
        );
    }

    public void destroy() {
        for (UUID markerId : pedalMarkers.values()) {
            removeIfPresent(markerId);
        }
        pedalMarkers.clear();
        slotLocations.clear();
        restoreClockBlock();
    }

    private void buildRelativeTo(Player player) {
        Location base = player.getLocation().clone();
        Vector forward = base.getDirection().setY(0).normalize();
        if (forward.lengthSquared() < 0.01) forward = new Vector(0, 0, 1);
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        for (int i = 0; i < 4; i++) {
            double offset = (i - 1.5) * 2.0;
            placeMarker(SlotOwner.PLAYER, i, base.clone()
                    .add(forward.clone().multiply(2.5))
                    .add(right.clone().multiply(offset)));
            placeMarker(SlotOwner.ENEMY, i, base.clone()
                    .add(forward.clone().multiply(5.5))
                    .add(right.clone().multiply(offset)));
            placeMarker(SlotOwner.ENEMY_PREVIEW, i, base.clone()
                    .add(forward.clone().multiply(8.0))
                    .add(right.clone().multiply(offset)));
        }
        placeClockBlock(base.clone()
                .add(forward.clone().multiply(3.5))
                .add(right.clone().multiply(-3.0)));
    }

    /** 在房间标点处放置/绑定真实 {@link Material#BELL} 方块（非展示实体）。 */
    private void placeClockBlock(Location configured) {
        if (configured == null || configured.getWorld() == null) return;

        Block block = configured.getBlock();
        clockBlockLocation = block.getLocation();
        clockLocation = block.getLocation().add(0.5, 0.5, 0.5);

        if (block.getType() == Material.BELL) {
            clockPlacedByArena = false;
            clockPreviousData = null;
            ensureFloorBell(block);
            return;
        }

        clockPreviousData = block.getBlockData().clone();
        clockPlacedByArena = true;
        block.setBlockData(floorBellData(), false);
    }

    private void restoreClockBlock() {
        if (clockBlockLocation == null) return;
        if (clockPlacedByArena && clockPreviousData != null) {
            Block block = clockBlockLocation.getBlock();
            if (block.getType() == Material.BELL) {
                block.setBlockData(clockPreviousData, false);
            }
        }
        clockBlockLocation = null;
        clockLocation = null;
        clockPreviousData = null;
        clockPlacedByArena = false;
    }

    private static BlockData floorBellData() {
        BlockData data = Material.BELL.createBlockData();
        if (data instanceof Bell bell) {
            bell.setAttachment(Bell.Attachment.FLOOR);
        }
        return data;
    }

    private static void ensureFloorBell(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Bell bell && bell.getAttachment() != Bell.Attachment.FLOOR) {
            bell.setAttachment(Bell.Attachment.FLOOR);
            block.setBlockData(bell, false);
        }
    }

    private void placeMarker(SlotOwner owner, int index, Location loc) {
        Location logicalCenter = ArenaCoordinates.toSlotCenter(loc);
        Location spawn = ArenaCoordinates.toPedalSpawn(loc);
        if (logicalCenter == null || spawn == null) return;

        SlotKey key = new SlotKey(owner, index);
        slotLocations.put(key, logicalCenter.clone());

        BlockData glass = pedalMaterial(owner).createBlockData();
        BlockDisplay pedal = spawn.getWorld().spawn(spawn, BlockDisplay.class, display -> {
            display.setBlock(glass);
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(pedalTransform());
            display.setBrightness(new Display.Brightness(15, 15));
            display.setPersistent(true);
            tagSlotEntity(display, owner, index);
        });

        pedalMarkers.put(key, pedal.getUniqueId());
    }

    private static Material pedalMaterial(SlotOwner owner) {
        return switch (owner) {
            case PLAYER -> Material.LIME_STAINED_GLASS;
            case ENEMY -> Material.RED_STAINED_GLASS;
            case ENEMY_PREVIEW -> Material.GRAY_STAINED_GLASS;
        };
    }

    /**
     * 薄玻璃踏板：XZ 0.8，Y 0.1。
     * 平移 -0.5Y 使缩放后薄板底面贴齐槽位高度（Display 以中心为原点）。
     */
    private static Transformation pedalTransform() {
        float yShift = -(PEDAL_THICKNESS / 2f);
        return new Transformation(
                new Vector3f(0f, yShift, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(ArenaCoordinates.PEDAL_WIDTH, PEDAL_THICKNESS, ArenaCoordinates.PEDAL_WIDTH),
                new AxisAngle4f(0f, 0f, 0f, 1f)
        );
    }

    private void tagSlotEntity(Entity entity, SlotOwner owner, int index) {
        entity.getPersistentDataContainer().set(InscriptionKeys.ARENA_ID, PersistentDataType.STRING, arenaId.toString());
        entity.getPersistentDataContainer().set(InscriptionKeys.SLOT_OWNER, PersistentDataType.STRING, owner.name());
        entity.getPersistentDataContainer().set(InscriptionKeys.SLOT_INDEX, PersistentDataType.INTEGER, index);
    }

    private static void removeIfPresent(UUID id) {
        if (id == null) return;
        Entity entity = findEntity(id);
        if (entity != null) entity.remove();
    }

    private static Entity findEntity(UUID id) {
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) return entity;
        }
        return null;
    }

    private record SlotKey(SlotOwner owner, int index) {}
}
