package com.github.mczju.mczjuscription.arena;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** 踏板交互音效（音符盒）。 */
public final class ArenaSlotSounds {

    public enum Kind {
        SELECT,
        CONFIRM,
        REJECT
    }

    private ArenaSlotSounds() {}

    public static void play(Player player, Kind kind) {
        if (player == null) {
            return;
        }
        switch (kind) {
            case SELECT -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.85f, 1.25f);
            case CONFIRM -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.9f, 1.6f);
            case REJECT -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.85f, 0.55f);
        }
    }
}
