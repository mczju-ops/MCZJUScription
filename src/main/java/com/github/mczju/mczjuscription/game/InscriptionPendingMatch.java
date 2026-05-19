package com.github.mczju.mczjuscription.game;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 离开大厅、再次 {@code joinGame(inscription)} 前写入的玩法参数。
 * 同一 MGC 游戏 ID 下区分「大厅实例」与「对局实例」。
 */
public final class InscriptionPendingMatch {

    private static final ConcurrentHashMap<UUID, PlayVariant> BY_LEADER = new ConcurrentHashMap<>();

    private InscriptionPendingMatch() {}

    public static void set(PlayerExt player, PlayVariant variant) {
        BY_LEADER.put(player.getUniqueId(), variant);
    }

    public static PlayVariant peek(PlayerExt player) {
        return BY_LEADER.get(player.getUniqueId());
    }

    /** 取出并清除，供等待策略在开局时使用。 */
    public static PlayVariant consume(PlayerExt player) {
        return BY_LEADER.remove(player.getUniqueId());
    }

    public static void clear(PlayerExt player) {
        BY_LEADER.remove(player.getUniqueId());
    }
}
