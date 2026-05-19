package com.github.mczju.mczjuscription.entity;

import com.github.mczju.mczjuscription.util.InscriptionKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

/** 对局内展示实体（土豆保底模型、飘字等）。 */
public final class MatchEntityDisplay {

    private static final float POTATO_SCALE = 0.85f;
    private static final double LABEL_HEIGHT = 0.55;

    private MatchEntityDisplay() {}

    public static BlockDisplay spawnPotatoModel(Location at) {
        Location spawn = at.clone();
        if (spawn.getWorld() == null) {
            return null;
        }
        return spawn
                .getWorld()
                .spawn(
                        spawn,
                        BlockDisplay.class,
                        display -> {
                            display.setBlock(Material.POTATO.createBlockData());
                            display.setBillboard(Display.Billboard.CENTER);
                            display.setTransformation(potatoTransform());
                            display.setBrightness(new Display.Brightness(15, 15));
                            display.setPersistent(true);
                            MatchEntityProtection.apply(display);
                            display
                                    .getPersistentDataContainer()
                                    .set(InscriptionKeys.MATCH_DISPLAY, PersistentDataType.BYTE, (byte) 1);
                        });
    }

    public static TextDisplay spawnLabel(Location anchor, Component text) {
        if (anchor.getWorld() == null) {
            return null;
        }
        return anchor
                .getWorld()
                .spawn(
                        anchor.clone().add(0, LABEL_HEIGHT, 0),
                        TextDisplay.class,
                        display -> {
                            display.text(text);
                            display.setBillboard(Display.Billboard.CENTER);
                            display.setSeeThrough(true);
                            display.setShadowed(true);
                            display.setDefaultBackground(false);
                            display.setPersistent(true);
                            MatchEntityProtection.apply(display);
                        });
    }

    public static void tagCreature(Entity entity, UUID instanceId) {
        entity.getPersistentDataContainer()
                .set(
                        InscriptionKeys.CREATURE_INSTANCE,
                        PersistentDataType.STRING,
                        instanceId.toString());
    }

    public static void tagWanderingTrader(Entity entity) {
        entity.getPersistentDataContainer()
                .set(InscriptionKeys.WANDERING_TRADER, PersistentDataType.BYTE, (byte) 1);
    }

    public static boolean isWanderingTrader(Entity entity) {
        return entity != null
                && entity.getPersistentDataContainer()
                        .has(InscriptionKeys.WANDERING_TRADER, PersistentDataType.BYTE);
    }

    private static Transformation potatoTransform() {
        float s = POTATO_SCALE;
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 0f, 1f),
                new Vector3f(s, s, s),
                new AxisAngle4f(0f, 0f, 0f, 1f));
    }
}
