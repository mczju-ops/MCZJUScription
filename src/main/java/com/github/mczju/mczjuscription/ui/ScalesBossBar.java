package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.Scales;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 对局期间用 BossBar 实时显示天平：进度 0 = 我方受击（-5），满格 = 敌方受击（+5）。
 * 每 tick 推送，避免仅在天平变化时才刷新客户端显示。
 */
public final class ScalesBossBar {

    private static final String TITLE_BASE = "§6⚖ §f天平 §7| §c我方 §7← §f§l%s§7 → §a敌方";

    private final InscriptionMatch match;
    private BossBar bossBar;
    private BukkitTask tickTask;

    public ScalesBossBar(InscriptionMatch match) {
        this.match = match;
    }

    public void start() {
        stop();
        bossBar = Bukkit.createBossBar(
                formatTitle(0),
                BarColor.YELLOW,
                BarStyle.SEGMENTED_10
        );
        bossBar.setProgress(0.5f);
        bossBar.setVisible(true);
        syncViewers();

        tickTask = Bukkit.getScheduler().runTaskTimer(
                MCZJUScriptionPlugin.getInstance(),
                this::tick,
                0L,
                1L
        );
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
    }

    private void tick() {
        if (bossBar == null || match.isMatchOver()) {
            stop();
            return;
        }

        syncViewers();

        Scales scales = match.scales();
        int value = scales.getValue();
        float progress = scales.bossBarProgress();

        bossBar.setProgress(progress);
        bossBar.setTitle(formatTitle(value));
        bossBar.setColor(colorFor(value));
    }

    private void syncViewers() {
        if (bossBar == null) return;

        Set<UUID> expected = new HashSet<>();
        for (ParticipantState human : match.humanParticipants()) {
            human.player().ifPresent(ext -> {
                Player player = ext.player();
                if (player.isOnline()) {
                    expected.add(player.getUniqueId());
                }
            });
        }

        for (Player viewer : new HashSet<>(bossBar.getPlayers())) {
            if (!expected.contains(viewer.getUniqueId())) {
                bossBar.removePlayer(viewer);
            }
        }

        for (UUID id : expected) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline() && !bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private static BarColor colorFor(int value) {
        if (value > 0) return BarColor.GREEN;
        if (value < 0) return BarColor.RED;
        return BarColor.YELLOW;
    }

    private static String formatTitle(int value) {
        String display;
        if (value > 0) {
            display = "+" + value;
        } else {
            display = String.valueOf(value);
        }
        return TITLE_BASE.formatted(display);
    }
}
