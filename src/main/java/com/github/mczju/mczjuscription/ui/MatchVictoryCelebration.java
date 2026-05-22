package com.github.mczju.mczjuscription.ui;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.utils.TextParser;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** 对局全胜后的正反馈与返回大厅倒计时。 */
public final class MatchVictoryCelebration {

    private static final int RETURN_SECONDS = 5;

    private MatchVictoryCelebration() {}

    public static void begin(List<PlayerExt> humans, Runnable onReturn) {
        for (int sec = RETURN_SECONDS; sec >= 1; sec--) {
            int shown = sec;
            Bukkit.getScheduler()
                    .runTaskLater(
                            MCZJUScriptionPlugin.getInstance(),
                            () -> showCountdown(humans, shown),
                            (RETURN_SECONDS - sec) * 20L);
        }

        Bukkit.getScheduler()
                .runTaskLater(MCZJUScriptionPlugin.getInstance(), onReturn, RETURN_SECONDS * 20L);
    }

    public static void presentWin(PlayerExt ext) {
        Player player = ext.player();
        if (!player.isOnline()) {
            return;
        }
        ext.sender()
                .success("<gold><bold>胜利！</bold></gold> <gray>你吹灭了对手的所有蜡烛。");
        player.showTitle(
                Title.title(
                        TextParser.parse("<gold><bold>胜利</bold></gold>"),
                        TextParser.parse("<yellow>对手的三根蜡烛已全部熄灭"),
                        Title.Times.times(
                                Duration.ofMillis(400), Duration.ofSeconds(3), Duration.ofMillis(600))));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, 1.2f);
    }

    public static void presentLoss(PlayerExt ext) {
        Player player = ext.player();
        if (!player.isOnline()) {
            return;
        }
        ext.sender().error("<red><bold>败北</bold></red> <gray>你的蜡烛已尽。");
        player.showTitle(
                Title.title(
                        TextParser.parse("<red><bold>败北</bold></red>"),
                        TextParser.parse("<gray>蜡烛已全部熄灭"),
                        Title.Times.times(
                                Duration.ofMillis(400), Duration.ofSeconds(2), Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.35f, 1.6f);
    }

    private static void showCountdown(List<PlayerExt> humans, int secondsLeft) {
        for (PlayerExt ext : humans) {
            if (!ext.player().isOnline()) {
                continue;
            }
            ext.actionBarSender()
                    .info("<gold>⚖ <white>%d<gold> 秒后回到大厅…".formatted(secondsLeft));
        }
    }
}
