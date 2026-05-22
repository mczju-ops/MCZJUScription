package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** UI 行 ItemDisplay 旋转：pitch(X) / yaw(Y) / roll(Z) 偏移，可写 room JSON 或局内 /isc arena ui 微调。 */
public final class ArenaUiIconTuning {

    /** 躺平基准：绕 X 轴 -90°。 */
    public static final float BASE_PITCH_DEG = -90f;

    private float pitchOffsetDeg;
    private float yawOffsetDeg;
    private float rollOffsetDeg;

    public ArenaUiIconTuning(float pitchOffsetDeg, float yawOffsetDeg, float rollOffsetDeg) {
        this.pitchOffsetDeg = pitchOffsetDeg;
        this.yawOffsetDeg = yawOffsetDeg;
        this.rollOffsetDeg = rollOffsetDeg;
    }

    public static ArenaUiIconTuning defaults() {
        return new ArenaUiIconTuning(0f, 0f, 0f);
    }

    public static ArenaUiIconTuning fromRoom(InscriptionGameRoom room) {
        if (room == null) {
            return defaults();
        }
        float pitch = room.arenaUiIconPitchDeg != null ? room.arenaUiIconPitchDeg : 0f;
        float yaw = room.arenaUiIconYawDeg != null ? room.arenaUiIconYawDeg : 0f;
        float roll = room.arenaUiIconRollDeg != null ? room.arenaUiIconRollDeg : 0f;
        return new ArenaUiIconTuning(pitch, yaw, roll);
    }

    public float pitchOffsetDeg() {
        return pitchOffsetDeg;
    }

    public float yawOffsetDeg() {
        return yawOffsetDeg;
    }

    public float rollOffsetDeg() {
        return rollOffsetDeg;
    }

    public void setPitchOffsetDeg(float pitchOffsetDeg) {
        this.pitchOffsetDeg = pitchOffsetDeg;
    }

    public void setYawOffsetDeg(float yawOffsetDeg) {
        this.yawOffsetDeg = yawOffsetDeg;
    }

    public void setRollOffsetDeg(float rollOffsetDeg) {
        this.rollOffsetDeg = rollOffsetDeg;
    }

    public void addPitchOffsetDeg(float delta) {
        this.pitchOffsetDeg += delta;
    }

    public void addYawOffsetDeg(float delta) {
        this.yawOffsetDeg += delta;
    }

    public void addRollOffsetDeg(float delta) {
        this.rollOffsetDeg += delta;
    }

    public void applyRoomDefaults(InscriptionGameRoom room) {
        if (room == null) {
            return;
        }
        pitchOffsetDeg = room.arenaUiIconPitchDeg != null ? room.arenaUiIconPitchDeg : 0f;
        yawOffsetDeg = room.arenaUiIconYawDeg != null ? room.arenaUiIconYawDeg : 0f;
        rollOffsetDeg = room.arenaUiIconRollDeg != null ? room.arenaUiIconRollDeg : 0f;
    }

    public Transformation flatIconTransform(ArenaOrientation orientation, float scale) {
        float pitch = BASE_PITCH_DEG + pitchOffsetDeg;
        float yaw = defaultYawForOrientation(orientation) + yawOffsetDeg;
        float roll = rollOffsetDeg;
        Quaternionf left =
                new Quaternionf()
                        .rotateX((float) Math.toRadians(pitch))
                        .rotateY((float) Math.toRadians(yaw))
                        .rotateZ((float) Math.toRadians(roll));
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                left,
                new Vector3f(scale, scale, scale),
                new Quaternionf());
    }

    /** 未加 room/局内偏移时的朝向基准（度，绕 Y）。 */
    static float defaultYawForOrientation(ArenaOrientation orientation) {
        return switch (orientation) {
            case SOUTH -> 90f;
            case NORTH -> 270f;
            case WEST -> 90f;
            case EAST -> 180f;
        };
    }
}
