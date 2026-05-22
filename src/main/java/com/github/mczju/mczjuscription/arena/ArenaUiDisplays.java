package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.session.ParticipantState;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** 11×11 场地 UI 行：文字 Billboard 朝向玩家，图标平贴踏板（旋转见 {@link ArenaUiIconTuning}）。 */
public final class ArenaUiDisplays {

    private static final float ICON_SCALE = 0.55f;
    private static final float RESOURCE_ICON_SCALE = ICON_SCALE * 2f;
    private static final float SHOP_ICON_SCALE = ICON_SCALE * 2.2f;
    private static final float TEXT_SCALE = 0.85f;
    private static final float COUNTER_TEXT_SCALE = TEXT_SCALE * 5f;
    private static final float SHOP_TEXT_SCALE = TEXT_SCALE * 3f;
    private static final double TEXT_Y_ABOVE = 0.24;
    private static final double ICON_Y_ABOVE = -0.04;
    /** 1 格纹理 = 16px；ItemDisplay 额外抬高 5px。 */
    private static final double ICON_PIXEL_LIFT = 5.0 / 16.0;

    private ArenaUiDisplays() {}

    public static TextDisplay spawnFlatCounter(Location center, Component text) {
        return spawnFlatCounter(center, text, ArenaOrientation.SOUTH, null);
    }

    public static TextDisplay spawnFlatCounter(
            Location center, Component text, ArenaOrientation orientation, ResolvedArenaLayout.UiSlotKind kind) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        boolean shop = kind == ResolvedArenaLayout.UiSlotKind.SHOP;
        float scale = shop ? SHOP_TEXT_SCALE : COUNTER_TEXT_SCALE;
        double textLift = shop ? TEXT_Y_ABOVE * 1.1 : TEXT_Y_ABOVE;
        Location spawn = uiSpawn(center, textLift);
        return spawn
                .getWorld()
                .spawn(
                        spawn,
                        TextDisplay.class,
                        display -> {
                            display.text(text);
                            display.setBillboard(Display.Billboard.CENTER);
                            display.setAlignment(TextDisplay.TextAlignment.CENTER);
                            display.setSeeThrough(true);
                            display.setShadowed(true);
                            display.setDefaultBackground(false);
                            display.setTransformation(scaleTransform(scale));
                            display.setBrightness(new Display.Brightness(15, 15));
                            display.setPersistent(true);
                        });
    }

    public static ItemDisplay spawnFlatIcon(Location center, Material material) {
        return spawnFlatIcon(center, material, ArenaOrientation.SOUTH, null, ArenaUiIconTuning.defaults());
    }

    public static ItemDisplay spawnFlatIcon(
            Location center,
            Material material,
            ArenaOrientation orientation,
            ResolvedArenaLayout.UiSlotKind kind,
            ArenaUiIconTuning iconTuning) {
        if (center == null || center.getWorld() == null || material == null) {
            return null;
        }
        ArenaUiIconTuning tuning = iconTuning != null ? iconTuning : ArenaUiIconTuning.defaults();
        float scale = iconScale(kind);
        double iconLift = (kind == ResolvedArenaLayout.UiSlotKind.SHOP ? ICON_Y_ABOVE * 0.85 : ICON_Y_ABOVE)
                + ICON_PIXEL_LIFT;
        Location spawn = uiSpawn(center, iconLift);
        return spawn
                .getWorld()
                .spawn(
                        spawn,
                        ItemDisplay.class,
                        display -> {
                            display.setItemStack(new org.bukkit.inventory.ItemStack(material));
                            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                            display.setBillboard(Display.Billboard.FIXED);
                            display.setTransformation(tuning.flatIconTransform(orientation, scale));
                            display.setBrightness(new Display.Brightness(15, 15));
                            display.setPersistent(true);
                        });
    }

    public static float iconScale(ResolvedArenaLayout.UiSlotKind kind) {
        if (kind == ResolvedArenaLayout.UiSlotKind.SHOP) {
            return SHOP_ICON_SCALE;
        }
        if (isResourceIcon(kind)) {
            return RESOURCE_ICON_SCALE;
        }
        return ICON_SCALE;
    }

    public static Component counterText(ResolvedArenaLayout.UiSlotKind kind, ParticipantState state) {
        if (state == null) {
            return Component.text("0");
        }
        var currency = state.currency();
        String value =
                switch (kind) {
                    case BLOOD -> String.valueOf(currency.getBlood());
                    case BONES -> String.valueOf(currency.getBones());
                    case FISH -> String.valueOf(currency.getFish());
                    case SHOP -> "商店";
                };
        return Component.text(value);
    }

    public static Material iconMaterial(ResolvedArenaLayout.UiSlotKind kind) {
        return switch (kind) {
            case BLOOD -> Material.ROTTEN_FLESH;
            case BONES -> Material.BONE;
            case FISH -> Material.COD;
            case SHOP -> Material.LEATHER;
        };
    }

    private static Location uiSpawn(Location center, double yAbove) {
        return center.clone().add(0, yAbove, 0);
    }

    private static boolean isResourceIcon(ResolvedArenaLayout.UiSlotKind kind) {
        return kind == ResolvedArenaLayout.UiSlotKind.BLOOD
                || kind == ResolvedArenaLayout.UiSlotKind.BONES
                || kind == ResolvedArenaLayout.UiSlotKind.FISH;
    }

    private static Transformation scaleTransform(float scale) {
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf());
    }
}
