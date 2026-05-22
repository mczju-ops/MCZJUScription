package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import org.bukkit.entity.Player;

/** {@code /isc arena ui icon ...} */
public final class ArenaUiIconCommandHandler {

    private ArenaUiIconCommandHandler() {}

    public static boolean handle(Player player, InscriptionMatch match, String[] args) {
        if (args.length < 4 || !"ui".equalsIgnoreCase(args[1]) || !"icon".equalsIgnoreCase(args[2])) {
            return false;
        }
        BattleArena arena = match.arena();
        if (arena == null) {
            player.sendMessage("§c请先 /isc arena 绑定场地。");
            return true;
        }
        String action = args[3].toLowerCase();
        ArenaUiIconTuning tuning = arena.iconTuning();
        return switch (action) {
            case "show" -> {
                player.sendMessage("§eUI 图标旋转（写入 room JSON）：");
                player.sendMessage("§7  arenaUiIconPitchDeg: §f" + format(tuning.pitchOffsetDeg())
                        + " §7(绕 X，相对 -90° 躺平)");
                player.sendMessage("§7  arenaUiIconYawDeg: §f" + format(tuning.yawOffsetDeg())
                        + " §7(绕 Y，相对默认朝向)");
                player.sendMessage("§7  arenaUiIconRollDeg: §f" + format(tuning.rollOffsetDeg())
                        + " §7(绕 Z，平面内滚转)");
                player.sendMessage("§7有效角度: pitch="
                        + format(ArenaUiIconTuning.BASE_PITCH_DEG + tuning.pitchOffsetDeg())
                        + "° yaw="
                        + format(ArenaUiIconTuning.defaultYawForOrientation(arena.orientation())
                                + tuning.yawOffsetDeg())
                        + "° roll="
                        + format(tuning.rollOffsetDeg())
                        + "°");
                yield true;
            }
            case "reset" -> {
                tuning.applyRoomDefaults(match.matchRoom());
                arena.refreshUiIconTransforms();
                player.sendMessage("§a已恢复为 room JSON 默认旋转。");
                yield true;
            }
            case "pitch", "yaw", "roll" -> {
                if (args.length < 5) {
                    player.sendMessage("§c用法: /isc arena ui icon " + action + " <角度|+5|-5>");
                    yield true;
                }
                float value = parseAngle(args[4]);
                if (Float.isNaN(value)) {
                    player.sendMessage("§c无效角度: " + args[4]);
                    yield true;
                }
                boolean relative = args[4].startsWith("+") || args[4].startsWith("-");
                switch (action) {
                    case "pitch" -> {
                        if (relative) {
                            tuning.addPitchOffsetDeg(value);
                        } else {
                            tuning.setPitchOffsetDeg(value);
                        }
                    }
                    case "yaw" -> {
                        if (relative) {
                            tuning.addYawOffsetDeg(value);
                        } else {
                            tuning.setYawOffsetDeg(value);
                        }
                    }
                    case "roll" -> {
                        if (relative) {
                            tuning.addRollOffsetDeg(value);
                        } else {
                            tuning.setRollOffsetDeg(value);
                        }
                    }
                    default -> { }
                }
                arena.refreshUiIconTransforms();
                syncRoom(match, tuning);
                player.sendMessage("§a已更新 "
                        + axisLabel(action)
                        + " 偏移为 "
                        + format(axisOffset(tuning, action))
                        + "°，可用 §f/isc arena ui icon show §a查看 JSON 字段。");
                yield true;
            }
            default -> {
                player.sendMessage("§c用法: /isc arena ui icon <pitch|yaw|roll|show|reset> [角度]");
                yield true;
            }
        };
    }

    private static String axisLabel(String action) {
        return switch (action) {
            case "pitch" -> "pitch(X)";
            case "yaw" -> "yaw(Y)";
            case "roll" -> "roll(Z)";
            default -> action;
        };
    }

    private static float axisOffset(ArenaUiIconTuning tuning, String action) {
        return switch (action) {
            case "pitch" -> tuning.pitchOffsetDeg();
            case "yaw" -> tuning.yawOffsetDeg();
            case "roll" -> tuning.rollOffsetDeg();
            default -> 0f;
        };
    }

    private static float parseAngle(String raw) {
        try {
            return Float.parseFloat(raw);
        } catch (NumberFormatException e) {
            return Float.NaN;
        }
    }

    private static String format(float value) {
        if (Math.abs(value - Math.round(value)) < 0.001f) {
            return String.valueOf(Math.round(value));
        }
        return String.format("%.2f", value);
    }

    private static void syncRoom(InscriptionMatch match, ArenaUiIconTuning tuning) {
        InscriptionGameRoom room = match.matchRoom();
        if (room == null) {
            return;
        }
        room.arenaUiIconPitchDeg = tuning.pitchOffsetDeg();
        room.arenaUiIconYawDeg = tuning.yawOffsetDeg();
        room.arenaUiIconRollDeg = tuning.rollOffsetDeg();
    }
}
