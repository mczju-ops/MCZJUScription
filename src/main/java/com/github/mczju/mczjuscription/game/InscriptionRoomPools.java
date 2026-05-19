package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 邪恶冥刻房间分区（同一 gameId {@link InscriptionGame#GAME_ID} 下多房间）。
 * <ul>
 *   <li>{@link #HUB} — 仅等待大厅，不参与对局分配</li>
 *   <li>{@link #SOLO} — {@code play10}～{@code play19} 单人</li>
 *   <li>{@link #DUEL} — {@code play20}～{@code play24} 双人</li>
 * </ul>
 */
public final class InscriptionRoomPools {

    public static final String HUB = "main";

    private static final int SOLO_FROM = 10;
    private static final int SOLO_TO = 19;
    private static final int DUEL_FROM = 20;
    private static final int DUEL_TO = 24;

    public static final Set<String> SOLO = playRange(SOLO_FROM, SOLO_TO);
    public static final Set<String> DUEL = playRange(DUEL_FROM, DUEL_TO);

    /** 主菜单 / {@code /isc hub} 新建实例时只允许占用大厅房间。 */
    public static final Set<String> HUB_ONLY = Set.of(HUB);

    private InscriptionRoomPools() {}

    public static Set<String> forVariant(PlayVariant variant) {
        return variant.isSolo() ? SOLO : DUEL;
    }

    public static String poolLabel(PlayVariant variant) {
        return variant.isSolo()
                ? "单人场地 play%d～play%d".formatted(SOLO_FROM, SOLO_TO)
                : "双人场地 play%d～play%d".formatted(DUEL_FROM, DUEL_TO);
    }

    private static Set<String> playRange(int from, int to) {
        Set<String> names = new LinkedHashSet<>();
        for (int i = from; i <= to; i++) {
            names.add("play" + i);
        }
        return Collections.unmodifiableSet(names);
    }
}
