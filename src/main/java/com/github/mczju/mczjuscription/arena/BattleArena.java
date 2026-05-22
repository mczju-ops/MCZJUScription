package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bell;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BattleArena {

    public enum PedalHighlight {
        NORMAL,
        SELECTED,
        CONFIRMED
    }

    private static final float PEDAL_THICKNESS = 0.1f;
    private static final String UI_SLOT_OWNER = "UI";
    private static final String STRIP_SLOT_OWNER = "STRIP";
    /** strip index: 0=preview, 1=bell */
    private static final int STRIP_INDEX_PREVIEW = 0;
    private static final int STRIP_INDEX_BELL = 1;

    private final UUID arenaId = UUID.randomUUID();
    private final UUID ownerId;
    private final Map<PedalKey, UUID> pedalMarkers = new HashMap<>();
    private final Map<PedalKey, Location> pedalCenters = new HashMap<>();
    private final Map<PedalKey, PedalHighlight> pedalHighlights = new HashMap<>();
    private final EnumMap<MatchSide, StagingProps> stagingProps = new EnumMap<>(MatchSide.class);
    private final EnumMap<MatchSide, UiRowProps> uiRows = new EnumMap<>(MatchSide.class);
    private StripProps previewStrip;
    private final EnumMap<MatchSide, StripProps> bellStrips = new EnumMap<>(MatchSide.class);

    private ResolvedArenaLayout layout;
    private ArenaOrientation orientation = ArenaOrientation.SOUTH;
    private ArenaUiIconTuning iconTuning = ArenaUiIconTuning.defaults();

    private BattleArena(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public static BattleArena atPlayer(Player player) {
        BattleArena arena = new BattleArena(player.getUniqueId());
        arena.buildRelativeTo(player);
        return arena;
    }

    public static BattleArena fromRoom(InscriptionGameRoom room, MatchMode mode, DeckMode deckMode) {
        ResolvedArenaLayout resolved = room.resolveLayout(mode);
        BattleArena arena = new BattleArena(UUID.randomUUID());
        arena.layout = resolved;
        arena.orientation = room.arenaOrientation();
        arena.iconTuning = ArenaUiIconTuning.fromRoom(room);
        for (SlotOwner owner : SlotOwner.values()) {
            for (int i = 0; i < 4; i++) {
                Location loc = resolved.slot(owner, i);
                if (loc != null) {
                    arena.placeBattlePedal(owner, i, loc);
                }
            }
        }
        if (resolved.hasUiRow(MatchSide.PLAYER)) {
            arena.setupUiRow(MatchSide.PLAYER, resolved);
        }
        if (resolved.hasUiRow(MatchSide.ENEMY)) {
            arena.setupUiRow(MatchSide.ENEMY, resolved);
        }
        arena.setupStaging(MatchSide.PLAYER, resolved.staging(MatchSide.PLAYER));
        if (resolved.staging(MatchSide.ENEMY) != null) {
            arena.setupStaging(MatchSide.ENEMY, resolved.staging(MatchSide.ENEMY));
        }
        arena.setupStrips(resolved, mode);
        return arena;
    }

    public ResolvedArenaLayout layout() {
        return layout;
    }

    public ArenaOrientation orientation() {
        return orientation;
    }

    public ArenaUiIconTuning iconTuning() {
        return iconTuning;
    }

    /** 局内微调 UI 图标旋转后刷新所有 ItemDisplay 变换。 */
    public void refreshUiIconTransforms() {
        for (UiRowProps props : uiRows.values()) {
            for (var entry : props.iconDisplays.entrySet()) {
                ItemDisplay display = entry.getValue();
                if (display == null || !display.isValid()) {
                    continue;
                }
                float scale = ArenaUiDisplays.iconScale(entry.getKey());
                display.setTransformation(iconTuning.flatIconTransform(orientation, scale));
            }
        }
    }

    /** 供 {@link ArenaPedalRaycast} 使用的踏板碰撞体积。 */
    public java.util.List<PedalHitVolume> pedalHitVolumes() {
        java.util.List<PedalHitVolume> volumes = new java.util.ArrayList<>();
        for (var entry : pedalCenters.entrySet()) {
            PedalKey key = entry.getKey();
            Location center = entry.getValue();
            ArenaPedalTarget target = toTarget(key);
            if (target == null || center == null) {
                continue;
            }
            double halfX;
            double halfZ;
            if (key instanceof PedalKey.PreviewStrip || key instanceof PedalKey.BellStrip) {
                float length = ArenaGridParser.INNER_STRIP_PEDAL_LENGTH;
                float depth = ArenaGridParser.STRIP_PEDAL_DEPTH;
                if (ArenaCoordinates.stripLengthAlongWorldZ(orientation)) {
                    halfX = depth / 2.0;
                    halfZ = length / 2.0;
                } else {
                    halfX = length / 2.0;
                    halfZ = depth / 2.0;
                }
            } else {
                halfX = ArenaCoordinates.PEDAL_HALF_WIDTH + 0.15;
                halfZ = ArenaCoordinates.PEDAL_HALF_WIDTH + 0.15;
            }
            volumes.add(new PedalHitVolume(target, center, halfX, halfZ));
        }
        return volumes;
    }

    public record PedalHitVolume(ArenaPedalTarget target, Location center, double halfX, double halfZ) {}

    public UUID arenaId() {
        return arenaId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public Location slotLocation(SlotOwner owner, int index) {
        return clone(pedalCenters.get(new PedalKey.Battle(owner, index)));
    }

    public Location uiSlotLocation(MatchSide side, ResolvedArenaLayout.UiSlotKind kind) {
        return clone(pedalCenters.get(new PedalKey.Ui(side, kind)));
    }

    public Location clockLocation(MatchSide side) {
        Location strip = bellStripLocation(side);
        if (strip != null) {
            return strip.clone();
        }
        StagingProps props = stagingProps.get(side);
        return props == null || props.clockLocation == null ? null : props.clockLocation.clone();
    }

    public boolean hasClock(MatchSide side) {
        return bellStripLocation(side) != null || hasLegacyClock(side);
    }

    public Location bellStripLocation(MatchSide side) {
        return clone(pedalCenters.get(new PedalKey.BellStrip(side)));
    }

    private boolean hasLegacyClock(MatchSide side) {
        StagingProps props = stagingProps.get(side);
        return props != null && props.clockLocation != null;
    }

    public boolean isClockBlock(Block block, MatchSide side) {
        StagingProps props = stagingProps.get(side);
        if (block == null || props == null || props.clockBlockLocation == null) {
            return false;
        }
        Location at = block.getLocation();
        return at.getBlockX() == props.clockBlockLocation.getBlockX()
                && at.getBlockY() == props.clockBlockLocation.getBlockY()
                && at.getBlockZ() == props.clockBlockLocation.getBlockZ()
                && at.getWorld().equals(props.clockBlockLocation.getWorld());
    }

    public SlotOwner resolveOwner(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(InscriptionKeys.SLOT_OWNER, PersistentDataType.STRING);
        if (raw == null || UI_SLOT_OWNER.equals(raw)) return null;
        return SlotOwner.valueOf(raw);
    }

    public int resolveIndex(Entity entity) {
        Integer idx = entity.getPersistentDataContainer().get(InscriptionKeys.SLOT_INDEX, PersistentDataType.INTEGER);
        return idx == null ? -1 : idx;
    }

    public ArenaPedalTarget resolvePedalTarget(Entity entity) {
        if (!isSlotMarker(entity)) {
            return null;
        }
        String ownerRaw = entity.getPersistentDataContainer().get(InscriptionKeys.SLOT_OWNER, PersistentDataType.STRING);
        if (ownerRaw == null) {
            return null;
        }
        Integer index = entity.getPersistentDataContainer().get(InscriptionKeys.SLOT_INDEX, PersistentDataType.INTEGER);
        if (index == null) {
            return null;
        }
        if (UI_SLOT_OWNER.equals(ownerRaw)) {
            String sideRaw =
                    entity.getPersistentDataContainer().get(InscriptionKeys.UI_SLOT_SIDE, PersistentDataType.STRING);
            ResolvedArenaLayout.UiSlotKind kind = ResolvedArenaLayout.UiSlotKind.fromIndex(index);
            if (sideRaw == null || kind == null) {
                return null;
            }
            try {
                return new ArenaPedalTarget.Ui(MatchSide.valueOf(sideRaw), kind);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        if (STRIP_SLOT_OWNER.equals(ownerRaw)) {
            if (index == STRIP_INDEX_PREVIEW) {
                return new ArenaPedalTarget.PreviewStrip();
            }
            if (index == STRIP_INDEX_BELL) {
                String sideRaw =
                        entity.getPersistentDataContainer().get(InscriptionKeys.UI_SLOT_SIDE, PersistentDataType.STRING);
                if (sideRaw == null) {
                    return null;
                }
                try {
                    return new ArenaPedalTarget.BellStrip(MatchSide.valueOf(sideRaw));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            return null;
        }
        try {
            return new ArenaPedalTarget.Battle(SlotOwner.valueOf(ownerRaw), index);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public boolean isSlotMarker(Entity entity) {
        if (!(entity instanceof Display)) return false;
        return arenaId.toString().equals(
                entity.getPersistentDataContainer().get(InscriptionKeys.ARENA_ID, PersistentDataType.STRING));
    }

    public void setPedalHighlight(ArenaPedalTarget target, PedalHighlight highlight) {
        PedalKey key = toKey(target);
        if (key == null) return;
        pedalHighlights.put(key, highlight);
        applyPedalMaterial(target, highlight);
    }

    private void applyPedalMaterial(ArenaPedalTarget target, PedalHighlight highlight) {
        PedalKey key = toKey(target);
        if (key == null) return;
        UUID markerId = pedalMarkers.get(key);
        if (markerId == null) return;
        Entity entity = findEntity(markerId);
        if (entity instanceof BlockDisplay display) {
            display.setBlock(pedalMaterial(target, highlight).createBlockData());
        }
    }

    public void flashPedal(ArenaPedalTarget target, PedalHighlight flash, PedalHighlight restore, long restoreDelayTicks) {
        setPedalHighlight(target, flash);
        new BukkitRunnable() {
            @Override
            public void run() {
                setPedalHighlight(target, restore);
            }
        }.runTaskLater(MCZJUScriptionPlugin.getInstance(), restoreDelayTicks);
    }

    public PedalHighlight normalHighlight(ArenaPedalTarget target) {
        return PedalHighlight.NORMAL;
    }

    /** @deprecated 使用 {@link #normalHighlight(ArenaPedalTarget)} */
    @Deprecated
    public PedalHighlight defaultHighlight(ArenaPedalTarget target) {
        return normalHighlight(target);
    }

    public void refreshUiDisplays(InscriptionMatch match) {
        if (match == null) {
            return;
        }
        refreshPreviewStrip(match);
        for (MatchSide side : MatchSide.values()) {
            UiRowProps props = uiRows.get(side);
            if (props == null) {
                continue;
            }
            var participant = match.participant(side);
            for (ResolvedArenaLayout.UiSlotKind kind : ResolvedArenaLayout.UiSlotKind.values()) {
                TextDisplay counter = props.counterDisplays.get(kind);
                if (counter != null && counter.isValid()) {
                    counter.text(ArenaUiDisplays.counterText(kind, participant));
                }
            }
        }
    }

    public void refreshPreviewStrip(InscriptionMatch match) {
        if (previewStrip == null || previewStrip.center == null || match == null) {
            return;
        }
        ArenaPreviewStripDisplay.sync(
                previewStrip.previewCreatures,
                match,
                this,
                previewStrip.center,
                orientation);
    }

    public void destroy() {
        for (UUID markerId : pedalMarkers.values()) {
            removeIfPresent(markerId);
        }
        pedalMarkers.clear();
        pedalCenters.clear();
        pedalHighlights.clear();
        for (UiRowProps props : uiRows.values()) {
            props.remove();
        }
        uiRows.clear();
        if (previewStrip != null) {
            previewStrip.remove();
            previewStrip = null;
        }
        for (StripProps props : bellStrips.values()) {
            props.remove();
        }
        bellStrips.clear();
        for (StagingProps props : stagingProps.values()) {
            props.restore();
        }
        stagingProps.clear();
    }

    private void setupUiRow(MatchSide side, ResolvedArenaLayout resolved) {
        UiRowProps props = new UiRowProps();
        uiRows.put(side, props);
        for (ResolvedArenaLayout.UiSlotKind kind : ResolvedArenaLayout.UiSlotKind.values()) {
            Location center = resolved.uiSlot(side, kind);
            if (center == null) {
                continue;
            }
            placeUiPedal(side, kind, center);
            Location logical = ArenaCoordinates.toSlotCenter(center);
            TextDisplay counter = ArenaUiDisplays.spawnFlatCounter(logical, Component.text("0"), orientation, kind);
            ItemDisplay icon =
                    ArenaUiDisplays.spawnFlatIcon(
                            logical, ArenaUiDisplays.iconMaterial(kind), orientation, kind, iconTuning);
            tagUiEntity(counter, side, kind);
            tagUiEntity(icon, side, kind);
            if (counter != null) {
                props.counterDisplays.put(kind, counter);
            }
            if (icon != null) {
                props.iconDisplays.put(kind, icon);
            }
        }
    }

    private void setupStrips(ResolvedArenaLayout resolved, MatchMode mode) {
        if (resolved.hasPreviewStrip()) {
            Location center = resolved.previewStripCenter();
            previewStrip = new StripProps();
            previewStrip.center = clone(center);
            placePreviewStrip(center);
        }
        if (resolved.hasBellStrip(MatchSide.PLAYER)) {
            placeBellStrip(MatchSide.PLAYER, resolved.bellStripCenter(MatchSide.PLAYER));
        }
        if (resolved.hasBellStrip(MatchSide.ENEMY)) {
            placeBellStrip(MatchSide.ENEMY, resolved.bellStripCenter(MatchSide.ENEMY));
        }
    }

    private void setupStaging(MatchSide side, ResolvedArenaLayout.StagingSites sites) {
        if (sites == null) {
            return;
        }
        if (sites.bellStripCenter != null) {
            return;
        }
        StagingProps props = new StagingProps();
        stagingProps.put(side, props);
        if (sites.clock != null) {
            placeClockBlock(props, sites.clock);
        }
    }

    private void buildRelativeTo(Player player) {
        Location base = player.getLocation().clone();
        org.bukkit.util.Vector forward = base.getDirection().setY(0).normalize();
        if (forward.lengthSquared() < 0.01) forward = new org.bukkit.util.Vector(0, 0, 1);
        org.bukkit.util.Vector right = forward.clone().crossProduct(new org.bukkit.util.Vector(0, 1, 0)).normalize();

        for (int i = 0; i < 4; i++) {
            double offset = (i - 1.5) * 3.0;
            placeBattlePedal(SlotOwner.PLAYER, i, base.clone()
                    .add(forward.clone().multiply(2.5))
                    .add(right.clone().multiply(offset)));
            placeBattlePedal(SlotOwner.ENEMY, i, base.clone()
                    .add(forward.clone().multiply(5.5))
                    .add(right.clone().multiply(offset)));
            placeBattlePedal(SlotOwner.ENEMY_PREVIEW, i, base.clone()
                    .add(forward.clone().multiply(8.0))
                    .add(right.clone().multiply(offset)));
        }
        StagingProps props = new StagingProps();
        stagingProps.put(MatchSide.PLAYER, props);
        placeClockBlock(props, base.clone()
                .add(forward.clone().multiply(3.5))
                .add(right.clone().multiply(-3.0)));
    }

    private void placeClockBlock(StagingProps props, Location configuredCenter) {
        if (configuredCenter == null || configuredCenter.getWorld() == null) return;

        Location corner = ArenaCoordinates.toBlockCorner(configuredCenter);
        if (corner == null) return;
        Block block = corner.getBlock();
        props.clockBlockLocation = corner;
        props.clockLocation = ArenaCoordinates.cornerToBlockCenter(corner);

        if (block.getType() == Material.BELL) {
            props.clockPlacedByArena = false;
            props.clockPreviousData = null;
            ensureFloorBell(block);
            return;
        }

        props.clockPreviousData = block.getBlockData().clone();
        props.clockPlacedByArena = true;
        block.setBlockData(floorBellData(), false);
    }

    private void placeBattlePedal(SlotOwner owner, int index, Location loc) {
        placePedal(new PedalKey.Battle(owner, index), loc, battleMaterial(owner), owner.name(), index, null);
    }

    private void placePreviewStrip(Location center) {
        if (center == null) {
            return;
        }
        Material material = Material.LIGHT_GRAY_STAINED_GLASS;
        placeInnerStripPedal(
                new PedalKey.PreviewStrip(),
                center,
                material,
                STRIP_INDEX_PREVIEW,
                null);
    }

    private void placeBellStrip(MatchSide side, Location center) {
        if (center == null) {
            return;
        }
        Material material = side == MatchSide.PLAYER ? Material.WHITE_STAINED_GLASS : Material.GRAY_STAINED_GLASS;
        placeInnerStripPedal(new PedalKey.BellStrip(side), center, material, STRIP_INDEX_BELL, side.name());
        bellStrips.put(side, new StripProps());
    }

    private void placeInnerStripPedal(
            PedalKey key, Location center, Material material, int stripIndex, String sideTag) {
        Location logicalCenter = ArenaCoordinates.toStripCenter(center);
        Location spawn = ArenaCoordinates.toInnerStripPedalSpawn(center, orientation);
        if (logicalCenter == null || spawn == null) {
            return;
        }
        pedalCenters.put(key, logicalCenter.clone());
        pedalHighlights.put(key, PedalHighlight.NORMAL);
        BlockDisplay pedal = spawn.getWorld().spawn(spawn, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(
                    stripPedalTransform(orientation, ArenaGridParser.INNER_STRIP_PEDAL_LENGTH));
            display.setBrightness(new Display.Brightness(15, 15));
            display.setPersistent(true);
            tagPedalEntity(display, STRIP_SLOT_OWNER, stripIndex, sideTag);
        });
        pedalMarkers.put(key, pedal.getUniqueId());
    }

    private static Transformation stripPedalTransform(ArenaOrientation orientation, float length) {
        float yShift = -(PEDAL_THICKNESS / 2f);
        float depth = ArenaGridParser.STRIP_PEDAL_DEPTH;
        float scaleX;
        float scaleZ;
        if (ArenaCoordinates.stripLengthAlongWorldZ(orientation)) {
            scaleX = depth;
            scaleZ = length;
        } else {
            scaleX = length;
            scaleZ = depth;
        }
        return new Transformation(
                new Vector3f(0f, yShift, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(scaleX, PEDAL_THICKNESS, scaleZ),
                new AxisAngle4f(0f, 0f, 0f, 1f));
    }

    private void placeUiPedal(MatchSide side, ResolvedArenaLayout.UiSlotKind kind, Location loc) {
        Material material = kind == ResolvedArenaLayout.UiSlotKind.SHOP
                ? Material.BLUE_STAINED_GLASS
                : Material.WHITE_STAINED_GLASS;
        placePedal(new PedalKey.Ui(side, kind), loc, material, UI_SLOT_OWNER, kind.index(), side.name());
    }

    private void placePedal(
            PedalKey key,
            Location loc,
            Material material,
            String ownerTag,
            int index,
            String uiSide) {
        Location logicalCenter = ArenaCoordinates.toSlotCenter(loc);
        Location spawn = ArenaCoordinates.toPedalSpawn(loc);
        if (logicalCenter == null || spawn == null) return;

        pedalCenters.put(key, logicalCenter.clone());
        pedalHighlights.put(key, PedalHighlight.NORMAL);

        BlockDisplay pedal = spawn.getWorld().spawn(spawn, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBillboard(Display.Billboard.FIXED);
            display.setTransformation(pedalTransform());
            display.setBrightness(new Display.Brightness(15, 15));
            display.setPersistent(true);
            tagPedalEntity(display, ownerTag, index, uiSide);
        });
        pedalMarkers.put(key, pedal.getUniqueId());
    }

    private static Material battleMaterial(SlotOwner owner) {
        return switch (owner) {
            case PLAYER -> Material.WHITE_STAINED_GLASS;
            case ENEMY, ENEMY_PREVIEW -> Material.GRAY_STAINED_GLASS;
        };
    }

    private Material pedalMaterial(ArenaPedalTarget target, PedalHighlight highlight) {
        return switch (highlight) {
            case SELECTED -> Material.YELLOW_STAINED_GLASS;
            case CONFIRMED -> Material.LIME_STAINED_GLASS;
            case NORMAL -> switch (target) {
                case ArenaPedalTarget.Battle battle -> battleMaterial(battle.owner());
                case ArenaPedalTarget.Ui ui -> ui.kind() == ResolvedArenaLayout.UiSlotKind.SHOP
                        ? Material.BLUE_STAINED_GLASS
                        : Material.WHITE_STAINED_GLASS;
                case ArenaPedalTarget.BellStrip bell -> bell.side() == MatchSide.PLAYER
                        ? Material.WHITE_STAINED_GLASS
                        : Material.GRAY_STAINED_GLASS;
                case ArenaPedalTarget.PreviewStrip ignored -> Material.LIGHT_GRAY_STAINED_GLASS;
            };
        };
    }

    private static Transformation pedalTransform() {
        float yShift = -(PEDAL_THICKNESS / 2f);
        return new Transformation(
                new Vector3f(0f, yShift, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(ArenaCoordinates.PEDAL_WIDTH, PEDAL_THICKNESS, ArenaCoordinates.PEDAL_WIDTH),
                new AxisAngle4f(0f, 0f, 0f, 1f));
    }

    private void tagPedalEntity(BlockDisplay entity, String ownerTag, int index, String uiSide) {
        entity.getPersistentDataContainer().set(InscriptionKeys.ARENA_ID, PersistentDataType.STRING, arenaId.toString());
        entity.getPersistentDataContainer().set(InscriptionKeys.SLOT_OWNER, PersistentDataType.STRING, ownerTag);
        entity.getPersistentDataContainer().set(InscriptionKeys.SLOT_INDEX, PersistentDataType.INTEGER, index);
        if (uiSide != null) {
            entity.getPersistentDataContainer().set(InscriptionKeys.UI_SLOT_SIDE, PersistentDataType.STRING, uiSide);
        }
    }

    private void tagUiEntity(Entity entity, MatchSide side, ResolvedArenaLayout.UiSlotKind kind) {
        if (entity == null) return;
        entity.getPersistentDataContainer().set(InscriptionKeys.ARENA_ID, PersistentDataType.STRING, arenaId.toString());
        entity.getPersistentDataContainer().set(InscriptionKeys.SLOT_OWNER, PersistentDataType.STRING, UI_SLOT_OWNER);
        entity.getPersistentDataContainer().set(InscriptionKeys.SLOT_INDEX, PersistentDataType.INTEGER, kind.index());
        entity.getPersistentDataContainer().set(InscriptionKeys.UI_SLOT_SIDE, PersistentDataType.STRING, side.name());
    }

    private static ArenaPedalTarget toTarget(PedalKey key) {
        return switch (key) {
            case PedalKey.Battle battle -> new ArenaPedalTarget.Battle(battle.owner(), battle.index());
            case PedalKey.Ui ui -> new ArenaPedalTarget.Ui(ui.side(), ui.kind());
            case PedalKey.BellStrip bell -> new ArenaPedalTarget.BellStrip(bell.side());
            case PedalKey.PreviewStrip ignored -> new ArenaPedalTarget.PreviewStrip();
        };
    }

    private static PedalKey toKey(ArenaPedalTarget target) {
        return switch (target) {
            case ArenaPedalTarget.Battle battle -> new PedalKey.Battle(battle.owner(), battle.index());
            case ArenaPedalTarget.Ui ui -> new PedalKey.Ui(ui.side(), ui.kind());
            case ArenaPedalTarget.BellStrip bell -> new PedalKey.BellStrip(bell.side());
            case ArenaPedalTarget.PreviewStrip ignored -> new PedalKey.PreviewStrip();
        };
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

    private static Location clone(Location loc) {
        return loc == null ? null : loc.clone();
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

    private sealed interface PedalKey permits PedalKey.Battle, PedalKey.Ui, PedalKey.BellStrip, PedalKey.PreviewStrip {
        record Battle(SlotOwner owner, int index) implements PedalKey {}

        record Ui(MatchSide side, ResolvedArenaLayout.UiSlotKind kind) implements PedalKey {}

        record BellStrip(MatchSide side) implements PedalKey {}

        record PreviewStrip() implements PedalKey {}
    }

    private final class StagingProps {
        private Location clockLocation;
        private Location clockBlockLocation;
        private BlockData clockPreviousData;
        private boolean clockPlacedByArena;

        private void restore() {
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
    }

    private static final class StripProps {
        private Location center;
        private final List<BoardCreature> previewCreatures = new ArrayList<>();

        private void remove() {
            ArenaPreviewStripDisplay.clear(previewCreatures);
            center = null;
        }
    }

    private static final class UiRowProps {
        private final EnumMap<ResolvedArenaLayout.UiSlotKind, TextDisplay> counterDisplays = new EnumMap<>(ResolvedArenaLayout.UiSlotKind.class);
        private final EnumMap<ResolvedArenaLayout.UiSlotKind, ItemDisplay> iconDisplays = new EnumMap<>(ResolvedArenaLayout.UiSlotKind.class);

        private void remove() {
            for (TextDisplay display : counterDisplays.values()) {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            }
            for (ItemDisplay display : iconDisplays.values()) {
                if (display != null && display.isValid()) {
                    display.remove();
                }
            }
            counterDisplays.clear();
            iconDisplays.clear();
        }
    }
}
